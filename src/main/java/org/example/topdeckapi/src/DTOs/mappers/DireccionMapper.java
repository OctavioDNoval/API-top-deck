package org.example.topdeckapi.src.DTOs.mappers;

import org.example.topdeckapi.src.DTOs.response.DireccionResponse;
import org.example.topdeckapi.src.model.Direccion;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UsuarioMapper.class})
public interface DireccionMapper {

    DireccionResponse toResponse(Direccion direccion);
}
