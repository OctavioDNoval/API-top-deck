package org.example.topdeckapi.src.DTOs.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DireccionDTO {
    private Long id_direccion;
    private UsuarioDTO usuarioDTO;
    private String ciudad;
    private String provincia;
    private String pais;
    private String codigo_postal;
    private String direccion;
    private String altura;
    private String piso;
}
