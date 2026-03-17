package org.example.topdeckapi.src.service.IMPL;

import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.mappers.DireccionMapper;
import org.example.topdeckapi.src.DTOs.request.DireccionRequest;
import org.example.topdeckapi.src.DTOs.response.DireccionResponse;
import org.example.topdeckapi.src.Enumerados.ROL;
import org.example.topdeckapi.src.Exception.UsuarioNotFoundException;
import org.example.topdeckapi.src.Repository.IDireccionRepo;
import org.example.topdeckapi.src.Repository.IUsuarioRepo;
import org.example.topdeckapi.src.model.Direccion;
import org.example.topdeckapi.src.model.Usuario;
import org.example.topdeckapi.src.service.Interface.IDireccionService;
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

    public List<DireccionResponse> getAll(){
        return direccionRepo.findAll().stream()
                .map(direccionMapper::toResponse)
                .collect(Collectors.toList());
    }

    public DireccionResponse getById(Long id){
        Direccion d= direccionRepo.findById(id)
                .orElseThrow(()-> new RuntimeException("Direccion no encontrado"));
        return direccionMapper.toResponse(d);
    }

    public DireccionResponse guardar(DireccionRequest dto) {
        Usuario usuario = usuarioRepo.findById(dto.getIdUsuario())
                .orElseThrow(()-> new UsuarioNotFoundException("Usuario no encontrado"));

        Direccion direccion = Direccion.builder()
                .usuario(usuario)
                .ciudad(dto.getCiudad())
                .provincia(dto.getProvincia())
                .pais(dto.getPais())
                .codigoPostal(dto.getCodigoPostal())
                .direccion(dto.getDireccion())
                .altura(dto.getAltura())
                .piso(dto.getPiso())
                .principal(dto.getPrincipal())
                .build();


        Direccion direccionGuardada = direccionRepo.save(direccion);
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

    public DireccionResponse update(DireccionRequest request, Long id) {
        return direccionRepo.findById(id)
                .map(d->{
                    direccionMapper.updateEntity(d, request);

                    if(request.getIdUsuario() != null && !request.getIdUsuario().equals(id)){
                        Usuario u = usuarioRepo.findById(request.getIdUsuario())
                                .orElseThrow(()-> new UsuarioNotFoundException("Usuario no encontrado"));

                        d.setUsuario(u);
                    }

                    Direccion direccionActualizada= direccionRepo.save(d);
                    return  direccionMapper.toResponse(direccionActualizada);
                })
                .orElseThrow(()-> new UsuarioNotFoundException("Usuario asignado a la direccion no encontrado"));
    }

    public List<DireccionResponse> direccionesPorUsuario (){
        Usuario u = usuarioService.obtenerUsuarioAutenticado();

        return direccionRepo.findByUsuario_IdUsuario(u.getIdUsuario())
                .stream()
                .map(direccionMapper::toResponse)
                .collect(Collectors.toList());
    }

    public boolean delete(Long id) {
        if (direccionRepo.existsById(id)) {
            direccionRepo.deleteById(id);
            return true;
        }else{
            return false;
        }
    }
}
