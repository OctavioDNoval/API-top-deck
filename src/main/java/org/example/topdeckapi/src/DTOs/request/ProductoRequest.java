package org.example.topdeckapi.src.DTOs.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductoRequest {
    private Long idCategoria;
    private Long idTag;
    private String nombre;
    private String descripcion;
    private Integer stock;
    private Double precio;
    private String imgUrl;
    private Integer descuento;
}
