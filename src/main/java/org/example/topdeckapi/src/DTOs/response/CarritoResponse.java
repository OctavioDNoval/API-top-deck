package org.example.topdeckapi.src.DTOs.response;

import java.math.BigDecimal;
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
    private String idCarrito;
    private LocalDateTime fechaCreacion;
    private BigDecimal precioTotal;
    List<DetalleCarritoResponse> detalleCarrito;

    //Datos resumidos del Usuario
    private String idUsuario;
    private String nombreUsuario;


}
