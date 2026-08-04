package org.example.topdeckapi.src.DTOs.mappers;

import org.example.topdeckapi.src.DTOs.request.DetalleCarritoRequest;
import org.example.topdeckapi.src.DTOs.response.DetalleCarritoResponse;
import org.example.topdeckapi.src.model.DetalleCarrito;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DetalleCarritoMapper {

    @Mapping(target = "idDetalleCarrito", source = "uuid")
    @Mapping(target = "idProducto", source = "producto.uuid")
    @Mapping(target = "nombreProducto", source = "producto.nombre")
    @Mapping(target = "precio", source = "producto.precio")
    @Mapping(target = "descuento", source = "producto.descuento")
    @Mapping(target = "imgUrl", source = "producto.imgUrl")
    DetalleCarritoResponse toResponse (DetalleCarrito detalleCarrito);

    @Mapping(target = "carrito", ignore = true)
    @Mapping(target = "producto", ignore = true)
    DetalleCarrito toEntity(DetalleCarritoRequest detalleCarritoRequest);
}
