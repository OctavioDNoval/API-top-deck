package org.example.topdeckapi.src.DTOs.CreateDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDate;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateEventoDTO {
    private String nombreEvento;
    private String ubicacion;
    private LocalDate fecha;
    private LocalTime hora;
    private Integer precioEntrada;
}
