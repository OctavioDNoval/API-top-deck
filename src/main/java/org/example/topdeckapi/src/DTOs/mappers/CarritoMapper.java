package org.example.topdeckapi.src.DTOs.mappers;

import org.example.topdeckapi.src.DTOs.request.CarritoRequest;
import org.example.topdeckapi.src.DTOs.response.CarritoResponse;
import org.example.topdeckapi.src.model.Carrito;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {
        DetalleCarritoMapper.class,
        UsuarioMapper.class,
})
public interface CarritoMapper {
    @Mapping(target = "idCarrito", source = "idCarrito")
    @Mapping(target = "fechaCreacion", source = "fechaCreacion")
    @Mapping(target = "detalleCarrito", source = "detalles")
    @Mapping(target = "idUsuario", source = "usuario.idUsuario")
    @Mapping(target = "nombreUsuario", source = "usuario.nombre")
    @Mapping(target = "sessionId", source = "sessionId")
    CarritoResponse toResponse(Carrito carrito);

    @Mapping(target = "idCarrito", ignore = true)
    Carrito toEntity(CarritoRequest carritoRequest);
}
