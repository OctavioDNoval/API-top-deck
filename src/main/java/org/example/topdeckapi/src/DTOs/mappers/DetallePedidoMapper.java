package org.example.topdeckapi.src.DTOs.mappers;

import org.example.topdeckapi.src.DTOs.request.CarritoRequest;
import org.example.topdeckapi.src.DTOs.request.DetallePedidoRequest;
import org.example.topdeckapi.src.DTOs.response.DetallePedidoResponse;
import org.example.topdeckapi.src.model.DetallePedido;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DetallePedidoMapper {

    @Mapping(target = "idProducto", source = "producto.idProducto")
    @Mapping(target = "nombreProducto", source = "producto.nombre")
    @Mapping(target = "precioProducto", source = "producto.precio")
    @Mapping(target = "descuentoProducto", source = "producto.descuento")
    @Mapping(target = "idPedido", source = "pedido.idPedido")
    DetallePedidoResponse toResponse(DetallePedido detallePedido);

    @Mapping(target = "idDetallePedido", ignore = true)
    @Mapping(target = "producto", ignore = true)
    @Mapping(target = "pedido", ignore = true)
    @Mapping(target = "subTotal", ignore = true)
    DetallePedido toEntity(DetallePedidoRequest detallePedidoRequest);
}
