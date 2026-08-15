package org.example.topdeckapi.src.DTOs.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.topdeckapi.src.Enumerados.ROL;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UsuarioRequest {
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, message = "El nombre debe tener al menos 2 caracteres")
    private String nombre;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato válido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;
    private String telefono;
    private String ipUsuario;
    private String versionTerminosYCondicionesAceptados = "0.0";
    private Boolean terminosAceptados = false;
    private String fechaAceptacionTerminos;
    private ROL rol;
}
