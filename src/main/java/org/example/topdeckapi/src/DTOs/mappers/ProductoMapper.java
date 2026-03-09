package org.example.topdeckapi.src.DTOs.mappers;

import org.example.topdeckapi.src.DTOs.request.ProductoRequest;
import org.example.topdeckapi.src.DTOs.response.ProductoResponse;
import org.example.topdeckapi.src.model.Categoria;
import org.example.topdeckapi.src.model.Producto;
import org.example.topdeckapi.src.model.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", uses = {
        CategoriaMapper.class,
        TagMapper.class,
})
public interface ProductoMapper {

    @Mapping(source = "nombre", target = "nombreProducto")
    ProductoResponse toResponse(Producto producto);

    @Mapping(target="categoria", ignore = true)
    @Mapping(target = "tag", ignore =true)
    @Mapping(target = "idProducto", ignore = true)
    Producto toEntity(ProductoRequest productoRequest);


}
