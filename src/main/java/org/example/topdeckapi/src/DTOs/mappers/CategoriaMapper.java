package org.example.topdeckapi.src.DTOs.mappers;

import org.example.topdeckapi.src.DTOs.request.CategoriaRequest;
import org.example.topdeckapi.src.DTOs.response.CategoriaResponse;
import org.example.topdeckapi.src.model.Categoria;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoriaMapper {

    CategoriaResponse toResponse(Categoria categoria);

    @Mapping(target = "idCategoria", ignore = true)
    Categoria toEntity(CategoriaRequest categoriaRequest);

}
