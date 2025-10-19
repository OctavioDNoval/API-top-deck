package org.example.topdeckapi.src.service.IMPL;

import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.DTO.DetallePedidoDTO;
import org.example.topdeckapi.src.Exception.ProductNotFoundException;
import org.example.topdeckapi.src.model.DetallePedido;
import org.example.topdeckapi.src.model.Pedido;
import org.example.topdeckapi.src.model.Producto;
import org.example.topdeckapi.src.service.Interface.IDetallePedidoService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DetallePedidoService implements IDetallePedidoService{
    private final IDetallePedidoService detallePedidoService;
    private final ProductoService productoService;


    protected DetallePedidoDTO convertEntityToDTO (DetallePedido dp){
        Long idProducto = dp.getProducto() != null ? dp.getProducto().getProductoId() : null;
        Long idPedido = dp.getPedido() != null ? dp.getPedido().getIdPedido() : null;

        return new DetallePedidoDTO(
                dp.getIdDetallePedido(),
                dp.getCantidad(),
                dp.getPrecioUnitario(),
                idProducto,
                idPedido
        );
    }

    protected DetallePedido convertDTOToEntity(DetallePedidoDTO dto, Pedido p){
        if(dto == null) return null;

        Optional<Producto> prod = productoService.buscarPorId(dto.getId_producto());
        Producto producto = prod.orElseThrow(()-> new ProductNotFoundException("Producto no encontrado"));

        DetallePedido dp = new DetallePedido();
        dp.setIdDetallePedido(dto.getId_detalle_pedido());
        dp.setCantidad(dto.getCantidad());
        dp.setPrecioUnitario(dto.getPrecio_unitario());
        dp.setProducto(producto);
        dp.setPedido(p);
        return dp;
    }
}
