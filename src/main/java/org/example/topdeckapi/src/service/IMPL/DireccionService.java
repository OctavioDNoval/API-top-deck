package org.example.topdeckapi.src.service.IMPL;

import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.CreateDTO.CreateDireccionDTO;
import org.example.topdeckapi.src.DTOs.DTO.DireccionDTO;
import org.example.topdeckapi.src.DTOs.DTO.UsuarioDTO;
import org.example.topdeckapi.src.DTOs.UpdateDTO.UpdateDireccionDTO;
import org.example.topdeckapi.src.Exception.UsuarioNotFoundException;
import org.example.topdeckapi.src.Repository.IDireccionRepo;
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
    private final UsuarioService usuarioService;

    protected Direccion convertToEntity(DireccionDTO dto) {
        return
                new Direccion(
                dto.getId_direccion(),
                usuarioService.convertToEntity(dto.getUsuarioDTO()),
                dto.getCiudad(),
                dto.getProvincia(),
                dto.getPais(),
                dto.getCodigo_postal(),
                dto.getDireccion(),
                dto.getAltura(),
                dto.getPiso(),
                        dto.getPrincipal()
        );
    }

    protected DireccionDTO convertToDTO(Direccion d) {
        return new DireccionDTO(
                d.getIdDireccion(),
                usuarioService.convertToDto(d.getUsuario()),
                d.getCiudad(),
                d.getProvincia(),
                d.getPais(),
                d.getCodigoPostal(),
                d.getDireccion(),
                d.getAltura(),
                d.getPiso(),
                d.getPrincipal()
        );
    }

    public List<DireccionDTO> getAll(){
        return direccionRepo.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Optional<DireccionDTO> getById(Long id){
        return direccionRepo.findById(id)
                .map(this::convertToDTO);
    }

    public DireccionDTO guardar(CreateDireccionDTO dto) {
        Usuario usuario = usuarioService.buscarEntidadPorId(dto.getId_usuario())
                .orElseThrow(() -> new UsuarioNotFoundException("Usuario id: "+ dto.getId_usuario()+"no encontrado"));

        Direccion direccion = new Direccion();
        direccion.setUsuario(usuario);
        direccion.setCiudad(dto.getCiudad());
        direccion.setProvincia(dto.getProvincia());
        direccion.setPais(dto.getPais());
        direccion.setCodigoPostal(dto.getCodigo_postal());
        direccion.setDireccion(dto.getDireccion());
        direccion.setAltura(dto.getAltura());
        direccion.setPiso(dto.getPiso());
        direccion.setPrincipal(dto.getPrincipal());

        Direccion saved = direccionRepo.save(direccion);
        return convertToDTO(saved);
    }

    public DireccionDTO guardarSinUsuario (CreateDireccionDTO dto) {
        Direccion direccion = new Direccion();
        direccion.setUsuario(usuarioService.buscarEntidadPorId(dto.getId_usuario()).orElse(null));
        direccion.setCiudad(dto.getCiudad());
        direccion.setProvincia(dto.getProvincia());
        direccion.setPais(dto.getPais());
        direccion.setCodigoPostal(dto.getCodigo_postal());
        direccion.setDireccion(dto.getDireccion());
        direccion.setAltura(dto.getAltura());
        direccion.setPiso(dto.getPiso());


        Direccion saved = direccionRepo.save(direccion);
        return convertToDTO(saved);
    }

    public Optional<DireccionDTO> update(UpdateDireccionDTO dto, Long id) {
        return direccionRepo.findById(id)
                .map(d -> {
                    if (dto.getCiudad() != null && !dto.getCiudad().isEmpty()) {
                        d.setCiudad(dto.getCiudad());
                    }
                    if (dto.getProvincia() != null && !dto.getProvincia().isEmpty()) {
                        d.setProvincia(dto.getProvincia());
                    }
                    if (dto.getPais() != null && !dto.getPais().isEmpty()) {
                        d.setPais(dto.getPais());
                    }
                    if (dto.getCodigo_postal() != null && !dto.getCodigo_postal().isEmpty()) {
                        d.setCodigoPostal(dto.getCodigo_postal());
                    }
                    if (dto.getDireccion() != null && !dto.getDireccion().isEmpty()) {
                        d.setDireccion(dto.getDireccion());
                    }
                    if (dto.getAltura() != null && !dto.getAltura().isEmpty()) {
                        d.setAltura(dto.getAltura());
                    }
                    if (dto.getPiso() != null && !dto.getPiso().isEmpty()) {
                        d.setPiso(dto.getPiso());
                    }
                    if (dto.getPrincipal() != null ) {
                        d.setPrincipal(dto.getPrincipal());
                    }
            Direccion updated = direccionRepo.save(d);
            return convertToDTO(updated);
        });
    }

    public List<DireccionDTO> direccionesPorUsuario (Long idUsuario){
        List<DireccionDTO> lista = direccionRepo.findByUsuario_IdUsuario(idUsuario)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return lista;
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
