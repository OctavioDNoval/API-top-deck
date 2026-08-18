package org.example.topdeckapi.src.DTOs.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NuevoEstadoRequest {
    @NotBlank(message = "El estado es obligatorio")
    @Pattern(regexp = "PENDIENTE|CONFIRMADO|RECHAZADO",
            message = "Estado inválido. Valores permitidos: PENDIENTE, CONFIRMADO, RECHAZADO")
    private String estado;
}
