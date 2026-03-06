package org.example.topdeckapi.src.DTOs.mappers;

import org.example.topdeckapi.src.DTOs.request.DireccionRequest;
import org.example.topdeckapi.src.DTOs.response.DireccionResponse;
import org.example.topdeckapi.src.model.Direccion;
import org.example.topdeckapi.src.model.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", uses = {UsuarioMapper.class})
public interface DireccionMapper {

    @Mapping(target = "idUsuario", source = "usuario.idUsuario")
    @Mapping(target = "nombreUsuario", source = "usuario.nombre")
    DireccionResponse toResponse(Direccion direccion);

    @Mapping(target = "idDireccion", ignore = true)
    @Mapping(target = "usuario", source = "idUsuario", qualifiedByName = "idToUsuario")
    Direccion toEntity(DireccionRequest direccionRequest);

    @Named("idToUsuario")
    default Usuario idToUsuario( Long idUsuario) {
        if (idUsuario == null) return null;

        Usuario usuario = new Usuario();
        usuario.setIdUsuario(idUsuario);
        return usuario;
    }
}
