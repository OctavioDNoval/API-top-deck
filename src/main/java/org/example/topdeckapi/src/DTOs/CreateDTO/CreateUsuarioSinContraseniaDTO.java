package org.example.topdeckapi.src.DTOs.CreateDTO;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUsuarioSinContraseniaDTO {
    @NotEmpty
    private String nombreCompleto;
    @NotEmpty
    private String email;

}
