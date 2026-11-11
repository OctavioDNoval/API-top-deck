package org.example.topdeckapi.src.DTOs.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/* Body of PATCH /eventos/admin/edit/{id}: only the fields sent are changed. The state is computed from the date. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventoUpdateRequest {
    @Pattern(regexp = ".*\\S.*", message = "El nombre del evento no puede estar vacío")
    @Size(max = 255, message = "El nombre puede tener hasta 255 caracteres")
    private String nombreEvento;

    @Pattern(regexp = ".*\\S.*", message = "La ubicación no puede estar vacía")
    @Size(max = 255, message = "La ubicación puede tener hasta 255 caracteres")
    private String ubicacion;

    private LocalDate fecha;

    private LocalTime hora;

    @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
    private BigDecimal precioEntrada;
}
