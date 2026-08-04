package org.example.topdeckapi.src.DTOs.mappers;

import org.example.topdeckapi.src.DTOs.request.UsuarioRequest;
import org.example.topdeckapi.src.DTOs.response.UsuarioResponse;
import org.example.topdeckapi.src.Enumerados.ROL;
import org.example.topdeckapi.src.model.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    @Mapping(target = "idUsuario", source = "uuid")
    UsuarioResponse toResponse(Usuario usuario);

    @Mapping(target = "password", ignore = true)
    Usuario toEntity(UsuarioRequest usuarioRequest);
}
