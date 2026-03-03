package org.example.topdeckapi.src.DTOs.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LogResponse {
    private Long idLog;
    private String nombreUsuario;
    private LocalDateTime fecha;
    private String accion;
    private String tablaAfectada;
}
