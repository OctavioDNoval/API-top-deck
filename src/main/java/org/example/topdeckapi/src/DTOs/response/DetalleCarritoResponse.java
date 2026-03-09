package org.example.topdeckapi.src.DTOs.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DetalleCarritoResponse {
    private Long idDetalleCarrito;
    private Integer cantidad;

    //Datos del producto resumido
    private Long idProducto;
    private String nombreProducto;
    private Double precio;
    private Integer descuento;
    private String imgUrl;
}
