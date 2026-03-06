package org.example.topdeckapi.src.DTOs.mappers;

import org.example.topdeckapi.src.DTOs.request.DetalleCarritoRequest;
import org.example.topdeckapi.src.DTOs.response.DetalleCarritoResponse;
import org.example.topdeckapi.src.model.DetalleCarrito;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DetalleCarritoMapper {

    @Mapping(target = "idProducto", source = "producto.idProducto")
    @Mapping(target = "nombreProducto", source = "producto.nombre")
    @Mapping(target = "precio", source = "producto.precio")
    @Mapping(target = "descuento", source = "producto.descuento")
    @Mapping(target = "imgUrl", source = "producto.imgUrl")
    DetalleCarritoResponse toResponse (DetalleCarrito detalleCarrito);

    @Mapping(target = "idDetalleCarrito", ignore = true)
    @Mapping(target = "carrito", ignore = true)
    @Mapping(target = "producto", ignore = true)
    //Ignoramos los campos de producto y carrito porque
    //En el equest solo vienen los ID que buscaremos la entidad
    //en el servicio
    DetalleCarrito toEntity(DetalleCarritoRequest detalleCarritoRequest);
}
