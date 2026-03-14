package org.example.topdeckapi.src.DTOs.mappers;

import org.example.topdeckapi.src.DTOs.request.CarritoRequest;
import org.example.topdeckapi.src.DTOs.response.CarritoResponse;
import org.example.topdeckapi.src.model.Carrito;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

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
    @Mapping(target = "precioTotal", source= "carrito", qualifiedByName = "calcularTotal")
    CarritoResponse toResponse(Carrito carrito);

    @Mapping(target = "idCarrito", ignore = true)
    Carrito toEntity(CarritoRequest carritoRequest);

    @Named("calcularTotal")
    default Double calcularTotal(Carrito carrito) {
        if(carrito.getDetalles() == null || carrito.getDetalles().isEmpty())
            return 0.0;
        return  carrito.getDetalles().stream()
                .mapToDouble(d->{
                    double precio = d.getProducto().getPrecio() != null ? d.getProducto().getPrecio() : 0.0;
                    int cantidad = d.getCantidad() != null? d.getCantidad() : 0;
                    double descuento = d.getProducto().getDescuento() != null ? (double) d.getProducto().getDescuento() / 100 : 0.0;
                    return precio* (1-descuento) *cantidad;
                })
                .sum();
    }
}
