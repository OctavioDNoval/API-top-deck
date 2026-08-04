package org.example.topdeckapi.src.DTOs.mappers;

import org.example.topdeckapi.src.DTOs.request.DireccionRequest;
import org.example.topdeckapi.src.DTOs.response.DireccionResponse;
import org.example.topdeckapi.src.model.Direccion;
import org.example.topdeckapi.src.model.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", uses = {UsuarioMapper.class})
public interface DireccionMapper {

    @Mapping(target = "idDireccion", source = "uuid")
    @Mapping(target = "idUsuario", source = "usuario.uuid")
    @Mapping(target = "nombreUsuario", source = "usuario.nombre")
    DireccionResponse toResponse(Direccion direccion);

    @Mapping(target = "usuario", source = "idUsuario", qualifiedByName = "idToUsuario")
    Direccion toEntity(DireccionRequest direccionRequest);

    @Mapping(target = "usuario", ignore = true)
    void updateEntity(@MappingTarget Direccion direccion, DireccionRequest request);

    @Named("idToUsuario")
    default Usuario idToUsuario(String uuidUsuario) {
        if (uuidUsuario == null) return null;

        Usuario usuario = new Usuario();
        usuario.setUuid(uuidUsuario);
        return usuario;
    }
}
