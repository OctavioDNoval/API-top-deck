package org.example.topdeckapi.src.DTOs.request;

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
public class PedidoRequest {
    private Long idUsuario;
    private Long idDireccion;
    private String ipUsuario;
    private List<DetallePedidoRequest> detalles;
}
