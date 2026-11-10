package org.example.topdeckapi.src.DTOs.response;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DetalleCarritoResponse {
    private String idDetalleCarrito;
    private Integer cantidad;

    //Datos del producto resumido
    private String idProducto;
    private String nombreProducto;
    private BigDecimal precio;
    private Integer descuento;
    private String imgUrl;
    // Current product stock, so the cart can cap the quantity it lets you pick.
    private Integer stock;
}
