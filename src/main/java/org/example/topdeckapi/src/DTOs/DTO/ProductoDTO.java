package org.example.topdeckapi.src.DTOs.DTO;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.topdeckapi.src.model.Categoria;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductoDTO {
    private Long idProducto;
    private String nombre;
    private String descripcion;
    private Float precio;
    private Integer stock;
    private String img_url;
    private String categoriaNombre;
    private Long categoriaId;
    private String tagNombre;
    private Long tagId;
}
