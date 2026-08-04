package org.example.topdeckapi.src.DTOs.mappers;

import org.example.topdeckapi.src.DTOs.request.CategoriaRequest;
import org.example.topdeckapi.src.DTOs.response.CategoriaResponse;
import org.example.topdeckapi.src.model.Categoria;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoriaMapper {

    @Mapping(target = "idCategoria", source = "uuid")
    CategoriaResponse toResponse(Categoria categoria);

    Categoria toEntity(CategoriaRequest categoriaRequest);

}
