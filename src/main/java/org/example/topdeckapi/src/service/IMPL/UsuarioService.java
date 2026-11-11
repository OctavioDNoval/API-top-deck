package org.example.topdeckapi.src.service.IMPL;

import org.example.topdeckapi.src.util.AuditMensaje;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.example.topdeckapi.src.DTOs.mappers.UsuarioMapper;
import org.example.topdeckapi.src.DTOs.request.PerfilUpdateRequest;
import org.example.topdeckapi.src.DTOs.request.UsuarioEfimeroRequest;
import org.example.topdeckapi.src.DTOs.request.UsuarioRequest;
import org.example.topdeckapi.src.DTOs.request.UsuarioUpdateRequest;
import org.example.topdeckapi.src.DTOs.response.PaginacionResponse;
import org.example.topdeckapi.src.DTOs.response.UsuarioResponse;
import org.example.topdeckapi.src.Enumerados.ROL;
import org.example.topdeckapi.src.Exception.BussinesException;
import org.example.topdeckapi.src.Exception.EmailYaRegistradoException;
import org.example.topdeckapi.src.Exception.ResourceNotFoundException;
import org.example.topdeckapi.src.Repository.ICarritoRepository;
import org.example.topdeckapi.src.Repository.IDireccionRepo;
import org.example.topdeckapi.src.Repository.IPedidoRepo;
import org.example.topdeckapi.src.Repository.IUsuarioRepo;
import org.example.topdeckapi.src.model.Direccion;
import org.example.topdeckapi.src.model.Pedido;
import org.example.topdeckapi.src.model.Usuario;
import org.example.topdeckapi.src.service.Interface.IUsuarioService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UsuarioService implements IUsuarioService {
    private final IUsuarioRepo usuarioRepo;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;
    private final PaginacionService paginacionService;
    private final AuditService auditService;
    private final ICarritoRepository carritoRepo;
    private final IDireccionRepo direccionRepo;
    private final IPedidoRepo pedidoRepo;

    //METODOS PARA OBTENER DATOS PAGINADOS Y DATOS PRECISOS

    private Sort buildSort (String sortBy, String direction){
        Map<String,String> mapeoCampos = Map.of(
                "idUsuario", "idUsuario",
                "nombre", "nombre",
                "email","email"
        );

        String campoReal = mapeoCampos.getOrDefault(sortBy,"idUsuario");
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, campoReal);
    }


    public PaginacionResponse<UsuarioResponse> obtenerPaginados (Integer pagina, Integer tamanio, String sortBy, String direction) {
        Sort sort = buildSort (sortBy, direction);
        Pageable pageable = PageRequest.of(pagina - 1, tamanio, sort);
        Page<Usuario> paginaUsuarios = usuarioRepo.findByFechaBajaIsNull(pageable);
        return paginacionService.crearPaginacionResponse(paginaUsuarios,pagina,tamanio, usuarioMapper::toResponse);
    }

    public PaginacionResponse<UsuarioResponse> obtenerPaginadosConFiltro(Integer pagina, Integer tamanio, String sortBy, String direction, String filtro) {
        Sort sort = buildSort (sortBy, direction);
        Pageable pageable = PageRequest.of(pagina - 1, tamanio, sort);
        Page<Usuario> paginaUsuarios = usuarioRepo.findBySearch(filtro, pageable);
        return paginacionService.crearPaginacionResponse(paginaUsuarios,pagina,tamanio, usuarioMapper::toResponse);
    }

    public UsuarioResponse obtenerPorId(String uuid){
        return usuarioMapper.toResponse(usuarioRepo.
                findByUuid(uuid)
                .filter(u -> u.getFechaBaja() == null)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Usuario no encontrado"))
        );
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public UsuarioResponse obtenerPerfilAutenticado(){
        Usuario usuario = obtenerUsuarioAutenticado();
        return usuarioMapper.toResponse(usuario);
    }

    /* The logged-in customer edits their own name and phone. */
    public UsuarioResponse actualizarPerfil(PerfilUpdateRequest request) {
        Usuario usuario = obtenerUsuarioAutenticado();
        if (usuario == null) {
            throw new ResourceNotFoundException("Usuario no autenticado");
        }
        String nombreAntes = usuario.getNombre();
        String telefonoAntes = usuario.getTelefono();
        usuario.setNombre(request.getNombre().trim());

        String telefono = request.getTelefono() == null ? "" : request.getTelefono().trim();
        if (telefono.isEmpty()) {
            usuario.setTelefono(null); // column is UNIQUE: "no phone" is null, not ""
        } else if (!telefono.equals(usuario.getTelefono())) {
            if (usuarioRepo.existsByTelefono(telefono)) {
                throw new BussinesException("Ese teléfono ya está asociado a otra cuenta");
            }
            usuario.setTelefono(telefono);
        }

        Usuario guardado = usuarioRepo.save(usuario);
        auditService.registrar("UPDATE", "usuario", guardado.getUuid(), new AuditMensaje.Cambios()
                .campo("nombre", nombreAntes, guardado.getNombre())
                .campo("teléfono", telefonoAntes, guardado.getTelefono())
                .texto("Editó su perfil"));
        return usuarioMapper.toResponse(guardado);
    }

    public Usuario obtenerUsuarioAutenticado(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            log.debug("No hay usuario autenticado o es anónimo");
            return null;
        }

        String email = auth.getName();
        log.debug("Email del usuario autenticado: {}", email);

        Usuario usuario = usuarioRepo.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Usuario no encontrado en BD con email: {}", email);
                    return new ResourceNotFoundException("Usuario autenticado no encontrado en el sistema");
                });

        log.debug("Usuario encontrado: {}, Rol: {}", usuario.getEmail(), usuario.getRol());
        return usuario;
    }

    //METODOS PARA CREAR DATOS
    public UsuarioResponse guardar (UsuarioRequest newUsuario){
        if(usuarioRepo.existsByEmail(newUsuario.getEmail())){
            throw new BussinesException("El email ya existe en el sistema");
        }

        Usuario usuario = usuarioMapper.toEntity(newUsuario);
        usuario.setPassword(passwordEncoder.encode(newUsuario.getPassword()));

        ROL rol = newUsuario.getRol() != null ? newUsuario.getRol() : ROL.USER;
        if (rol == ROL.GUEST) {
            throw new BussinesException("No se puede crear un usuario con rol GUEST");
        }
        usuario.setRol(rol);

        Usuario usuarioGuardado = usuarioRepo.save(usuario);
        auditService.registrar("INSERT", "usuario", usuarioGuardado.getUuid(), "Alta desde el panel con rol " + usuarioGuardado.getRol());
        return usuarioMapper.toResponse(usuarioGuardado);
    }

    /*
    * Partial update: only non-null fields change. Terms acceptance, IP and the
    * verification token are left as they are.
    * */
    public UsuarioResponse actualizarUsuario(UsuarioUpdateRequest request, String uuid){
        Usuario usuario = usuarioRepo.findByUuid(uuid)
                .filter(u -> u.getFechaBaja() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        validarCambioDeRol(usuario, request.getRol());
        String nombreAntes = usuario.getNombre();
        String emailAntes = usuario.getEmail();
        String telefonoAntes = usuario.getTelefono();
        ROL rolAntes = usuario.getRol();

        if (request.getEmail() != null) {
            String email = request.getEmail().trim();
            if (email.isEmpty()) {
                throw new BussinesException("El email no puede estar vacío");
            }
            if (usuarioRepo.existsByEmailAndIdUsuarioNot(email, usuario.getIdUsuario())) {
                throw new BussinesException("El email ya existe en el sistema");
            }
            usuario.setEmail(email);
        }
        if (request.getNombre() != null) {
            usuario.setNombre(request.getNombre().trim());
        }
        if (request.getTelefono() != null) {
            // Column is UNIQUE: store "no phone" as null, not "".
            String telefono = request.getTelefono().trim();
            usuario.setTelefono(telefono.isEmpty() ? null : telefono);
        }
        // A new password or a role change closes the user's open sessions.
        if (request.getRol() != null && request.getRol() != usuario.getRol()) {
            usuario.setRol(request.getRol());
            usuario.invalidarSesiones();
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(request.getPassword()));
            usuario.invalidarSesiones();
        }

        Usuario usuarioGuardado = usuarioRepo.save(usuario);
        boolean cambioPassword = request.getPassword() != null && !request.getPassword().isBlank();
        auditService.registrar("UPDATE", "usuario", usuarioGuardado.getUuid(), new AuditMensaje.Cambios()
                .campo("nombre", nombreAntes, usuarioGuardado.getNombre())
                .campo("email", emailAntes, usuarioGuardado.getEmail())
                .campo("teléfono", telefonoAntes, usuarioGuardado.getTelefono())
                .valor("rol", rolAntes, usuarioGuardado.getRol())
                .nota(cambioPassword, "contraseña")
                .nota(cambioPassword || rolAntes != usuarioGuardado.getRol(), "sesiones cerradas")
                .texto("Edición desde el panel"));
        return usuarioMapper.toResponse(usuarioGuardado);
    }

    public Usuario crearUsuarioEfimero(UsuarioEfimeroRequest newUsuario){
        Optional<Usuario> usuarioExistente = usuarioRepo.findByEmail(newUsuario.getEmail());

        if (usuarioExistente.isPresent()) {
            Usuario usuario = usuarioExistente.get();
            // An unconfirmed account may have been registered by someone else, so it
            // must not block the real owner from buying as a guest with their email.
            if (usuario.getRol() == ROL.GUEST || !usuario.isEmailConfirmado()) {
                return usuario;
            }
            throw new EmailYaRegistradoException("El email pertenece a una cuenta ya registrada, Intente ingresando a su cuenta");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(newUsuario.getNombre());
        usuario.setEmail(newUsuario.getEmail());
        usuario.setIpUsuario(newUsuario.getIpUsuario());
        usuario.setVersionTerminosYCondicionesAceptados(newUsuario.getVersionTerminosYCondicionesAceptados());
        usuario.setTerminosAceptados(newUsuario.getTerminosAceptados());
        usuario.setRol(ROL.GUEST);
        Usuario guardado = usuarioRepo.save(usuario);
        auditService.registrar("INSERT", "usuario", guardado.getUuid(), "Alta de cliente invitado (primer pedido sin cuenta)");
        return guardado;
    }

    public enum ResultadoBaja { ELIMINADO, ANONIMIZADO }

    /*
     * Without orders the user is removed with their cart and addresses. With orders
     * the account is anonymized and hidden instead, so the orders (and stats) keep
     * their owner; only the addresses those orders point to are kept.
     */
    public ResultadoBaja deleteUsuario(String uuid){
        Usuario usuario = usuarioRepo.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        if (usuario.getFechaBaja() != null) {
            throw new ResourceNotFoundException("Usuario no encontrado");
        }
        if (esUsuarioAutenticado(usuario)) {
            throw new BussinesException("No podés eliminar tu propia cuenta");
        }
        if (usuario.getRol() == ROL.ADMIN && usuarioRepo.countByRol(ROL.ADMIN) <= 1) {
            throw new BussinesException("No se puede eliminar al único administrador");
        }

        carritoRepo.findByUsuario(usuario).ifPresent(carritoRepo::delete);
        List<Pedido> pedidos = pedidoRepo.findByUsuario_IdUsuario(usuario.getIdUsuario());
        List<Direccion> direcciones = direccionRepo.findByUsuario_IdUsuario(usuario.getIdUsuario());
        auditService.registrar("DELETE", "usuario", usuario.getUuid(), pedidos.isEmpty()
                ? "Cuenta eliminada con su carrito y " + AuditMensaje.plural(direcciones.size(), "dirección", "direcciones")
                : "Cuenta anonimizada: se conservaron " + AuditMensaje.plural(pedidos.size(), "pedido", "pedidos"));

        if (pedidos.isEmpty()) {
            direccionRepo.deleteAll(direcciones);
            usuarioRepo.delete(usuario);
            return ResultadoBaja.ELIMINADO;
        }

        Set<Long> usadasEnPedidos = pedidos.stream()
                .map(Pedido::getDireccion)
                .filter(Objects::nonNull)
                .map(Direccion::getIdDireccion)
                .collect(Collectors.toSet());
        direccionRepo.deleteAll(direcciones.stream()
                .filter(d -> !usadasEnPedidos.contains(d.getIdDireccion()))
                .toList());
        anonimizar(usuario);
        usuarioRepo.save(usuario);
        return ResultadoBaja.ANONIMIZADO;
    }

    private void anonimizar(Usuario usuario) {
        usuario.setNombre("Usuario eliminado");
        // Unique and not null: a reserved domain that can never receive mail.
        usuario.setEmail("eliminado-" + usuario.getUuid() + "@topdeck.invalid");
        usuario.setTelefono(null);
        usuario.setPassword(null);
        usuario.setRol(ROL.GUEST); // GUEST can't log in or be claimed with this email
        usuario.setIpUsuario(null);
        usuario.setTokenVerificacionHash(null);
        usuario.setTokenVerificacionExpira(null);
        usuario.setTokenResetHash(null);
        usuario.setTokenResetExpira(null);
        usuario.invalidarSesiones();
        usuario.setFechaBaja(LocalDateTime.now());
    }

    // Keeps the panel from being left without an owner.
    private void validarCambioDeRol(Usuario usuario, ROL nuevoRol) {
        if (nuevoRol == null || nuevoRol == usuario.getRol()) return;
        if (esUsuarioAutenticado(usuario)) {
            throw new BussinesException("No podés cambiar tu propio rol");
        }
        if (usuario.getRol() == ROL.ADMIN && usuarioRepo.countByRol(ROL.ADMIN) <= 1) {
            throw new BussinesException("No se puede quitar el rol al único administrador");
        }
    }

    private boolean esUsuarioAutenticado(Usuario usuario) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && usuario.getEmail() != null
                && usuario.getEmail().equalsIgnoreCase(auth.getName());
    }
}
