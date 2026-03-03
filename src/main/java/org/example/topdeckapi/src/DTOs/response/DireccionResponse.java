package org.example.topdeckapi.src.DTOs.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DireccionResponse {
    private Long idDireccion;
    private String ciudad;
    private String provincia;
    private String codigoPostal;
    private String altura;
    private String direccion;
    private String pais;
    private String piso;
    private Boolean principal;

    //Datos del usuario asignado a la direccion
    private Long idUsuario;
    private String nombreUsuario;
}
