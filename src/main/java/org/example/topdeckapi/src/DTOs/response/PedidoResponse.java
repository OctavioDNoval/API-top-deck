package org.example.topdeckapi.src.DTOs.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.topdeckapi.src.Enumerados.ESTADO_PEDIDO;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PedidoResponse {
    private Long idPedido;
    private LocalDateTime fechaPedido;
    private ESTADO_PEDIDO estado;
    private Double total;

    //Datos del usuario
    private UsuarioResponse usuario;
    private String ipUsuario;

    //Datos de la direccion
    private DireccionResponse direccion;

    //Detalles del pedido
    private List<DetallePedidoResponse> detallePedidos;
}
