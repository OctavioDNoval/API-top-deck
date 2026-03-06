package org.example.topdeckapi.src.DTOs.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CarritoResponse {
    private Long idCarrito;
    private LocalDateTime fechaCreacion;
    List<DetalleCarritoResponse> detalleCarrito;

    //Datos resumidos del Usuario
    private Long idUsuario;
    private String nombreUsuario;
    private String sessionId;


}
