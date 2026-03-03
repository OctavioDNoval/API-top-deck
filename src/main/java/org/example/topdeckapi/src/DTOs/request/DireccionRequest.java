package org.example.topdeckapi.src.DTOs.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DireccionRequest {
    private Long idUsuario;
    private String ciudad;
    private String provincia;
    private String codigoPostal;
    private String altura;
    private String direccion;
    private String pais;
    private String piso;
    private Boolean principal;
}
