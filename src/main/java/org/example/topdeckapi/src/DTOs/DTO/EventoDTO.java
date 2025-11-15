package org.example.topdeckapi.src.DTOs.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.topdeckapi.src.Enumerados.ESTADO_EVENTO;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventoDTO {
    private Long idEvento;
    private String nombreEvento;
    private String ubicacion;


    private String fecha;


    private String hora;
    private Integer precioEntrada;
    private ESTADO_EVENTO estado;
}
