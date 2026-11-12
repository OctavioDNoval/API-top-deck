package org.example.topdeckapi.src.DTOs.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CambiarEmailRequest {
    @NotBlank(message = "El email nuevo es obligatorio")
    @Email(message = "El email no tiene un formato válido")
    @Size(max = 255, message = "El email es demasiado largo")
    private String nuevoEmail;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
}
