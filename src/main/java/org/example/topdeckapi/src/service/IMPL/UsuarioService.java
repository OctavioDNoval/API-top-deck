package org.example.topdeckapi.src.service.IMPL;

import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.CreateDTO.CreateUsuarioDTO;
import org.example.topdeckapi.src.DTOs.DTO.UsuarioDTO;
import org.example.topdeckapi.src.DTOs.UpdateDTO.UpdateUsuarioDTO;
import org.example.topdeckapi.src.Repository.IUsuarioRepo;
import org.example.topdeckapi.src.model.Producto;
import org.example.topdeckapi.src.model.Usuario;
import org.example.topdeckapi.src.service.Interface.IUsuarioService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService implements IUsuarioService {
    private final IUsuarioRepo usuarioRepo;

    protected UsuarioDTO convertToDto(Usuario u){
        return new UsuarioDTO(
                u.getIdUsuario(),
                u.getNombre(),
                u.getEmail(),
                u.getTelefono()
        );
    }

    protected Usuario createUsuarioDTOToEntity(CreateUsuarioDTO dto){
        return new Usuario(
                dto.getNombre(),
                dto.getEmail(),
                dto.getPassword(),
                dto.getTelefono()
        );
    }

    public List<UsuarioDTO> findAll(){
        return usuarioRepo.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public UsuarioDTO guardar (CreateUsuarioDTO newUsuario){
        Usuario u= createUsuarioDTOToEntity(newUsuario);
        Usuario usuarioCargado = usuarioRepo.save(u);
        return convertToDto(usuarioCargado);
    }

    public Optional<UsuarioDTO> buscarPorId(Long id){
        return usuarioRepo.findById(id)
                .map(this::convertToDto);
    }

    public Optional<UsuarioDTO> actualizarUsuario(UpdateUsuarioDTO dto, Long id){
        return usuarioRepo.findById(id)
                .map(u ->{
                    if((dto.getNombre()!=null)  &&  (!dto.getNombre().trim().isEmpty())){
                        u.setNombre(dto.getNombre());
                    }
                    if((dto.getEmail()!=null)  &&  (!dto.getEmail().trim().isEmpty())){
                        u.setEmail(dto.getEmail());
                    }
                    if((dto.getTelefono()!=null)  &&  (!dto.getTelefono().trim().isEmpty())){
                        u.setTelefono(dto.getTelefono());
                    }
                    Usuario usuarioActualizado = usuarioRepo.save(u);
                    return convertToDto(usuarioActualizado);
                });
    }

    public boolean deleteUsuario(Long id){
        if(usuarioRepo.existsById(id)){
            usuarioRepo.deleteById(id);
            return true;
        }else{
            return false;
        }
    }
}
