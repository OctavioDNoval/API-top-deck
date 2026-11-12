package org.example.topdeckapi.src.DTOs.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfirmarCambioEmailRequest {
    @NotBlank(message = "El token es obligatorio")
    private String token;
}
