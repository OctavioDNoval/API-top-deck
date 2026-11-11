package org.example.topdeckapi.src.service.IMPL;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.mappers.DireccionMapper;
import org.example.topdeckapi.src.DTOs.request.DireccionRequest;
import org.example.topdeckapi.src.DTOs.response.DireccionResponse;
import org.example.topdeckapi.src.Enumerados.ROL;
import org.example.topdeckapi.src.Exception.BussinesException;
import org.example.topdeckapi.src.Exception.ResourceNotFoundException;
import org.example.topdeckapi.src.Exception.UsuarioNotFoundException;
import org.example.topdeckapi.src.Repository.IDireccionRepo;
import org.example.topdeckapi.src.Repository.IPedidoRepo;
import org.example.topdeckapi.src.Repository.IUsuarioRepo;
import org.example.topdeckapi.src.model.Direccion;
import org.example.topdeckapi.src.model.Usuario;
import org.example.topdeckapi.src.service.Interface.IDireccionService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DireccionService implements IDireccionService {
    private final IDireccionRepo direccionRepo;
    private final DireccionMapper direccionMapper;
    private final IUsuarioRepo usuarioRepo;
    private final UsuarioService usuarioService;
    private final AuditService auditService;
    private final IPedidoRepo pedidoRepo;

    @Transactional(readOnly = true)
    public List<DireccionResponse> getAll(){
        return direccionRepo.findAll().stream()
                .map(direccionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DireccionResponse getById(String uuid){
        Direccion d= direccionRepo.findByUuid(uuid)
                .orElseThrow(()-> new ResourceNotFoundException("Direccion no encontrado"));
        return direccionMapper.toResponse(d);
    }

    public DireccionResponse guardar(DireccionRequest dto) {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        if (usuario == null) {
            throw new UsuarioNotFoundException("Usuario no autenticado");
        }

        Optional<Direccion> duplicada = direccionRepo
                .findByDireccionAndAlturaAndPisoAndCiudadAndProvinciaAndPaisAndCodigoPostalAndUsuario_IdUsuario(
                        dto.getDireccion(),
                        dto.getAltura(),
                        dto.getPiso(),
                        dto.getCiudad(),
                        dto.getProvincia(),
                        dto.getPais(),
                        dto.getCodigoPostal(),
                        usuario.getIdUsuario()
                );
        if (duplicada.isPresent() && Boolean.TRUE.equals(duplicada.get().getActiva())) {
            throw new BussinesException("Ya tenés una dirección con los mismos datos");
        }

        // The first address is the main one; marking a new one as main unmarks the rest.
        boolean esPrimera = direccionRepo.findByUsuario_IdUsuarioAndActivaTrue(usuario.getIdUsuario()).isEmpty();
        boolean principal = Boolean.TRUE.equals(dto.getPrincipal()) || esPrimera;
        if (principal) {
            desmarcarPrincipales(usuario.getIdUsuario(), null);
        }

        // Same address removed earlier (kept for its orders): bring it back.
        if (duplicada.isPresent()) {
            Direccion oculta = duplicada.get();
            oculta.setActiva(true);
            oculta.setPrincipal(principal);
            auditService.registrar("UPDATE", "direccion", oculta.getUuid(), "Volvió a agregar una dirección que había eliminado" + (principal ? " (principal)" : ""));
            return direccionMapper.toResponse(direccionRepo.save(oculta));
        }

        Direccion direccion = Direccion.builder()
                .usuario(usuario)
                .ciudad(dto.getCiudad())
                .provincia(dto.getProvincia())
                .pais(dto.getPais())
                .codigoPostal(dto.getCodigoPostal())
                .direccion(dto.getDireccion())
                .altura(dto.getAltura())
                .piso(dto.getPiso())
                .principal(principal)
                .build();


        Direccion direccionGuardada = direccionRepo.save(direccion);
        auditService.registrar("INSERT", "direccion", direccionGuardada.getUuid(), "Agregó una dirección" + (principal ? " (principal)" : ""));
        return direccionMapper.toResponse(direccionGuardada);
    }

    public Direccion guardarDireccionParaGuest (DireccionRequest dto, Usuario usuario) {

        Optional<Direccion> direccionExistente = direccionRepo.findByDireccionAndAlturaAndPisoAndCiudadAndProvinciaAndPaisAndCodigoPostalAndUsuario_IdUsuario(
                dto.getDireccion(),
                dto.getAltura(),
                dto.getPiso(),
                dto.getCiudad(),
                dto.getProvincia(),
                dto.getPais(),
                dto.getCodigoPostal(),
                usuario.getIdUsuario()
        );

        if(direccionExistente.isPresent()){
            return direccionExistente.get();
        }

        Direccion direccion = Direccion.builder()
                .usuario(usuario)
                .ciudad(dto.getCiudad())
                .provincia(dto.getProvincia())
                .pais(dto.getPais())
                .codigoPostal(dto.getCodigoPostal())
                .direccion(dto.getDireccion())
                .altura(dto.getAltura())
                .piso(dto.getPiso())
                .principal(dto.getPrincipal() != null ? dto.getPrincipal() : false)
                .build();

        return direccionRepo.save(direccion);
    }

    /*
     * An address that orders point to is never changed in place: the old one is
     * hidden (the orders keep it) and the edit is saved as a new address.
     */
    public DireccionResponse update(DireccionRequest request, String uuid) {
        Direccion d = propiaActiva(uuid);
        boolean principal = request.getPrincipal() != null ? request.getPrincipal() : Boolean.TRUE.equals(d.getPrincipal());

        Direccion destino = d;
        if (pedidoRepo.existsByDireccion_IdDireccion(d.getIdDireccion())) {
            d.setActiva(false);
            d.setPrincipal(false);
            direccionRepo.save(d);
            destino = Direccion.builder().usuario(d.getUsuario()).build();
        }
        destino.setDireccion(request.getDireccion());
        destino.setAltura(request.getAltura());
        destino.setPiso(request.getPiso());
        destino.setCiudad(request.getCiudad());
        destino.setProvincia(request.getProvincia());
        destino.setCodigoPostal(request.getCodigoPostal());
        destino.setPais(request.getPais());
        destino.setPrincipal(principal);

        if (principal) {
            desmarcarPrincipales(d.getUsuario().getIdUsuario(), d.getIdDireccion());
        }
        Direccion guardada = direccionRepo.save(destino);
        auditService.registrar("UPDATE", "direccion", guardada.getUuid(), destino == d
                ? "Editó una dirección"
                : "Editó una dirección usada en pedidos: se guardó como nueva y la anterior quedó para esos pedidos");
        return direccionMapper.toResponse(guardada);
    }

    public DireccionResponse marcarPrincipal(String uuid) {
        Direccion d = propiaActiva(uuid);
        desmarcarPrincipales(d.getUsuario().getIdUsuario(), d.getIdDireccion());
        d.setPrincipal(true);
        auditService.registrar("UPDATE", "direccion", d.getUuid(), "Marcó una dirección como principal");
        return direccionMapper.toResponse(direccionRepo.save(d));
    }

    @Transactional(readOnly = true)
    public List<DireccionResponse> direccionesPorUsuario (){
        Usuario u = usuarioService.obtenerUsuarioAutenticado();
        if (u == null) {
            throw new ResourceNotFoundException("Usuario no autenticado");
        }

        return direccionRepo.findByUsuario_IdUsuarioAndActivaTrue(u.getIdUsuario())
                .stream()
                .map(direccionMapper::toResponse)
                .collect(Collectors.toList());
    }

    /*
     * Removed for good when no order uses it; otherwise hidden so the orders keep it.
     * If it was the main one, the next remaining address becomes the main one.
     */
    public void delete(String uuid) {
        Direccion d = propiaActiva(uuid);
        Long idUsuario = d.getUsuario().getIdUsuario();
        Long idDireccion = d.getIdDireccion();
        boolean eraPrincipal = Boolean.TRUE.equals(d.getPrincipal());

        boolean usadaEnPedidos = pedidoRepo.existsByDireccion_IdDireccion(idDireccion);
        if (usadaEnPedidos) {
            d.setActiva(false);
            d.setPrincipal(false);
            direccionRepo.save(d);
        } else {
            direccionRepo.delete(d);
        }
        auditService.registrar("DELETE", "direccion", uuid, usadaEnPedidos
                ? "Eliminó una dirección usada en pedidos (quedó oculta para conservarlos)"
                : "Eliminó una dirección");

        if (eraPrincipal) {
            direccionRepo.findByUsuario_IdUsuarioAndActivaTrue(idUsuario).stream()
                    .filter(x -> !x.getIdDireccion().equals(idDireccion))
                    .findFirst()
                    .ifPresent(x -> {
                        x.setPrincipal(true);
                        direccionRepo.save(x);
                    });
        }
    }

    // The caller's own visible address: 404 if missing or removed, 403 if it belongs to someone else.
    private Direccion propiaActiva(String uuid) {
        Direccion d = direccionRepo.findByUuid(uuid)
                .filter(x -> Boolean.TRUE.equals(x.getActiva()))
                .orElseThrow(() -> new ResourceNotFoundException("Dirección no encontrada"));
        Usuario usuarioAuth = usuarioService.obtenerUsuarioAutenticado();
        if (usuarioAuth == null || !d.getUsuario().getIdUsuario().equals(usuarioAuth.getIdUsuario())) {
            throw new AccessDeniedException("No tenés acceso a esta dirección");
        }
        return d;
    }

    private void desmarcarPrincipales(Long idUsuario, Long excepto) {
        for (Direccion dir : direccionRepo.findByUsuario_IdUsuarioAndActivaTrue(idUsuario)) {
            if (!dir.getIdDireccion().equals(excepto) && Boolean.TRUE.equals(dir.getPrincipal())) {
                dir.setPrincipal(false);
                direccionRepo.save(dir);
            }
        }
    }
}
