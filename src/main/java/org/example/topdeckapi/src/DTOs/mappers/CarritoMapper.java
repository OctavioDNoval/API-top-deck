package org.example.topdeckapi.src.DTOs.mappers;

import java.math.BigDecimal;
import org.example.topdeckapi.src.util.Montos;
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
    @Mapping(target = "idCarrito", source = "uuid")
    @Mapping(target = "fechaCreacion", source = "fechaCreacion")
    @Mapping(target = "detalleCarrito", source = "detalles")
    @Mapping(target = "idUsuario", source = "usuario.uuid")
    @Mapping(target = "nombreUsuario", source = "usuario.nombre")
    @Mapping(target = "sessionId", source = "sessionId")
    @Mapping(target = "precioTotal", source= "carrito", qualifiedByName = "calcularTotal")
    CarritoResponse toResponse(Carrito carrito);

    Carrito toEntity(CarritoRequest carritoRequest);

    @Named("calcularTotal")
    default BigDecimal calcularTotal(Carrito carrito) {
        if(carrito.getDetalles() == null || carrito.getDetalles().isEmpty())
            return Montos.redondear(BigDecimal.ZERO);
        return carrito.getDetalles().stream()
                .map(d -> Montos.subtotal(d.getProducto().getPrecio(), d.getProducto().getDescuento(), d.getCantidad()))
                .reduce(Montos.redondear(BigDecimal.ZERO), BigDecimal::add);
    }
}
