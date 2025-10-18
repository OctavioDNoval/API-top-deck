package org.example.topdeckapi.src.service.IMPL;

import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.DTO.DetallePedidoDTO;
import org.example.topdeckapi.src.model.DetallePedido;
import org.example.topdeckapi.src.service.Interface.IDetallePedidoService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DetallePedidoService implements IDetallePedidoService{
    private final IDetallePedidoService detallePedidoService;

    protected DetallePedidoDTO convertEntityToDTO (DetallePedido dp){
        return new DetallePedidoDTO(
                dp.getIdDetallePedido(),
                dp.getCantidad(),
                dp.getPrecioUnitario(),
                dp.getPedido().getIdPedido(),
                dp.getPedido().getIdPedido()
        );
    }
}
