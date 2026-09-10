package org.example.topdeckapi.src.DTOs.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductoResponse {
    //Datos del producto
    private String idProducto;
    private String nombreProducto;
    private String descripcion;
    private Double precio;
    private Integer stock;
    private String imgUrl;
    private Integer descuento;
    private Boolean activo;
    private String tipoProducto;

    //Datos de la categoria
    private CategoriaResponse categoria;

    //Datos del tag
    private TagResponse tag;

    //Atributos de singles (solo para tipoProducto = SINGLE)
    private Map<String, Object> atributos;
}
