package org.example.topdeckapi.src.DTOs.response;

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
