package org.example.topdeckapi.src.DTOs.CreateDTO;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateDireccionDTO {
    private Long id_usuario;

    @NotBlank
    private String ciudad;

    @NotBlank
    private String provincia;

    @NotBlank
    private String pais;

    @NotBlank
    private String codigo_postal;

    @NotBlank
    private String direccion;

    @NotBlank
    private String altura;

    private String piso;
}
