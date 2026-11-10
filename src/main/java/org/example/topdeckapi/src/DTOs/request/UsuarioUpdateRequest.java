package org.example.topdeckapi.src.DTOs.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.topdeckapi.src.Enumerados.ROL;

/*
* Body of PATCH /user/admin/{id}. Every field is optional: only the ones sent
* are changed. Terms acceptance and IP are never touched here, since they are
* the user's own legal record.
* */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioUpdateRequest {
    @Pattern(regexp = ".*\\S.*\\S.*", message = "El nombre debe tener al menos 2 caracteres")
    private String nombre;

    @Email(message = "El email no tiene un formato válido")
    private String email;

    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;

    private String telefono;

    private ROL rol;
}
