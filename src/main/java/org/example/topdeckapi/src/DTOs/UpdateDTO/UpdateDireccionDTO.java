package org.example.topdeckapi.src.DTOs.UpdateDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateDireccionDTO {
    private String ciudad;
    private String provincia;
    private String pais;
    private String codigo_postal;
    private String direccion;
    private String altura;
    private String piso;
}
