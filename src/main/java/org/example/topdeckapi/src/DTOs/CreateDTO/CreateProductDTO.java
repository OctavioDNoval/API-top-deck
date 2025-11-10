package org.example.topdeckapi.src.DTOs.CreateDTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateProductDTO {
    @NotBlank
    private String nombre;
    private String descripcion;
    @Min(value=1)
    private Float precio;
    @Min(value=0)
    private Integer stock;
    @NotNull
    private Long id_categoria;
    private String img_url;

}
