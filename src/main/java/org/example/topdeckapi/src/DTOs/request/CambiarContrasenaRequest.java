package org.example.topdeckapi.src.DTOs.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/* Body of POST /user/me/password. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CambiarContrasenaRequest {
    @NotBlank(message = "Ingresá tu contraseña actual")
    private String actual;

    @NotBlank(message = "Ingresá la contraseña nueva")
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
    private String nueva;
}
