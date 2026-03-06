package org.example.topdeckapi.src.DTOs.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.topdeckapi.src.Enumerados.ESTADO_EVENTO;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EventoResponse {
    private Long idEvento;
    private String nombreEvento;
    private String ubicacion;
    private LocalDateTime fecha;
    private LocalTime hora;
    private Double precioEntrada;
    private ESTADO_EVENTO estado;
}
