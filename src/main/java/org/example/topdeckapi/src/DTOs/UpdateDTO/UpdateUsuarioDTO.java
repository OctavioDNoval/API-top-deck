package org.example.topdeckapi.src.DTOs.UpdateDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUsuarioDTO {
    private String nombre;
    private String email;
    private String password;
    private String telefono;
}
