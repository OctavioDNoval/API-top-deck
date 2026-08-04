package org.example.topdeckapi.src.DTOs.mappers;

import org.example.topdeckapi.src.DTOs.request.DetallePedidoRequest;
import org.example.topdeckapi.src.DTOs.response.DetallePedidoResponse;
import org.example.topdeckapi.src.model.DetallePedido;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DetallePedidoMapper {

    @Mapping(target = "idDetallePedido", source = "uuid")
    @Mapping(target = "idProducto", source = "producto.uuid")
    @Mapping(target = "nombreProducto", source = "producto.nombre")
    @Mapping(target = "precioProducto", source = "producto.precio")
    @Mapping(target = "descuentoProducto", source = "producto.descuento")
    @Mapping(target = "idPedido", source = "pedido.uuid")
    DetallePedidoResponse toResponse(DetallePedido detallePedido);

    @Mapping(target = "producto", ignore = true)
    @Mapping(target = "pedido", ignore = true)
    @Mapping(target = "subTotal", ignore = true)
    DetallePedido toEntity(DetallePedidoRequest detallePedidoRequest);
}
