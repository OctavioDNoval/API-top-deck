package org.example.topdeckapi.src.DTOs.CreateDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.topdeckapi.src.Enumerados.ESTADO_EVENTO;

import java.time.LocalTime;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateEventoDTO {
    private String nombreEvento;
    private String ubicacion;
    private Date fecha;
    private LocalTime hora;
    private Integer precioEntrada;
}
