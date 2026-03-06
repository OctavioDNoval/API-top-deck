package org.example.topdeckapi.src.DTOs.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DetallePedidoResponse {
    private Long idDetallePedido;
    private Integer cantidad;
    private Double precioUnitario;
    private Double subTotal;

    private Long idPedido;

    //Datos resumidos del producto
    private Long idProducto;
    private String nombreProducto;
    private Double precioProducto;
    private Integer descuentoProducto;
}
