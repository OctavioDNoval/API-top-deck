package org.example.topdeckapi.src.DTOs.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.topdeckapi.src.Enumerados.ESTADO_EVENTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EventoRequest {
    private String nombreEvento;
    private String ubicacion;
    private LocalDate fecha;
    private LocalTime hora;
    private Double precioEntrada;
    private ESTADO_EVENTO estado;
}
