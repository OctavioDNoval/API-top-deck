package org.example.topdeckapi.src.DTOs.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * Body of PATCH /user/me: what a customer can change on their own account.
 * Email changes are not allowed here (they would need confirming the new address).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PerfilUpdateRequest {
    @NotBlank(message = "El nombre es obligatorio")
    @Pattern(regexp = ".*\\S.*\\S.*", message = "El nombre debe tener al menos 2 caracteres")
    @Size(max = 100, message = "El nombre puede tener hasta 100 caracteres")
    private String nombre;

    // Same rule as the checkout; empty removes it.
    @Pattern(regexp = "^$|^[+]?[0-9 ()-]{8,20}$", message = "Ingresá un teléfono válido (solo números, espacios, guiones o +)")
    private String telefono;
}
