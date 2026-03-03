package org.example.topdeckapi.src.DTOs.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.topdeckapi.src.Enumerados.ESTADO_PEDIDO;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PedidoResponse {
    private Long idPedido;
    private LocalDateTime fechaPedido;
    private ESTADO_PEDIDO estadoPedido;
    private Double total;

    //Datos del usuario
    private UsuarioResponse usuario;

    //Datos de la direccion
    private DireccionResponse direccion;
}
