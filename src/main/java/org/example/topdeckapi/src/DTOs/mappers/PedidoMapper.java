package org.example.topdeckapi.src.DTOs.mappers;

import org.example.topdeckapi.src.DTOs.request.PedidoRequest;
import org.example.topdeckapi.src.DTOs.response.PedidoResponse;
import org.example.topdeckapi.src.model.Pedido;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {
        UsuarioMapper.class,
        DetallePedidoMapper.class,
        DireccionMapper.class,
})
public interface PedidoMapper {

    @Mapping(target = "detallePedidos", source = "detalles")
    PedidoResponse toResponse(Pedido pedido);

    @Mapping(target = "idPedido", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "direccion", ignore = true)
    @Mapping(target = "fechaPedido", ignore = true)
    @Mapping(target = "estado", constant = "PENDIENTE")
    @Mapping(target = "total", ignore = true)
    @Mapping(target = "detalles", ignore = true)
    Pedido toEntity(PedidoRequest pedidoRequest);
}
