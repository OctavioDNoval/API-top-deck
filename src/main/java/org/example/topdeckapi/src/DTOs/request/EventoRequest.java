package org.example.topdeckapi.src.DTOs.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.topdeckapi.src.Enumerados.ESTADO_EVENTO;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EventoRequest {
    @NotBlank(message = "El nombre del evento es obligatorio")
    private String nombreEvento;

    @NotBlank(message = "La ubicación es obligatoria")
    private String ubicacion;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    @NotNull(message = "La hora es obligatoria")
    private LocalTime hora;

    private Double precioEntrada;
    private ESTADO_EVENTO estado;
}
