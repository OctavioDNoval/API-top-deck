package org.example.topdeckapi.src.DTOs.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DireccionRequest {
    private String idUsuario;

    @NotBlank(message = "La ciudad es obligatoria")
    private String ciudad;

    @NotBlank(message = "La provincia es obligatoria")
    private String provincia;

    @NotBlank(message = "El código postal es obligatorio")
    private String codigoPostal;

    @NotBlank(message = "La altura es obligatoria")
    private String altura;

    @NotBlank(message = "La calle es obligatoria")
    private String direccion;

    private String pais;
    private String piso;
    private Boolean principal;
}
