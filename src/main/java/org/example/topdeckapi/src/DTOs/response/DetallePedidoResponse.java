package org.example.topdeckapi.src.DTOs.response;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DetallePedidoResponse {
    private String idDetallePedido;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subTotal;

    private String idPedido;

    //Datos resumidos del producto
    private String idProducto;
    private String nombreProducto;
    private BigDecimal precioProducto;
    private Integer descuentoProducto;
}
