package org.example.topdeckapi.src.DTOs.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductoResponse {
    //Datos del producto
    private Long idProducto;
    private String nombreProducto;
    private String descripcion;
    private Double precio;
    private Integer stock;
    private String imgUrl;
    private Integer descuento;
    private Boolean activo;

    //Datos de la categoria
    private CategoriaResponse categoria;

    //Datos del tag
    private TagResponse tag;
}
