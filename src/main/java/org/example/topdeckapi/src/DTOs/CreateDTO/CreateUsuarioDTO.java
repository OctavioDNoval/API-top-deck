package org.example.topdeckapi.src.DTOs.CreateDTO;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.topdeckapi.src.DTOs.DTO.DireccionDTO;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateUsuarioDTO {
    @NotBlank
    private String nombre;

    @NotBlank
    private String password;

    @NotBlank
    private String email;

    private String telefono;

    private DireccionDTO direccion;
}
