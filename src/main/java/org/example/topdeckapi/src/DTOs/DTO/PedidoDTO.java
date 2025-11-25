package org.example.topdeckapi.src.DTOs.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.topdeckapi.src.Enumerados.ESTADO_EVENTO;
import org.example.topdeckapi.src.model.DetallePedido;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PedidoDTO {
    private Long id_pedido;
    private UsuarioDTO usuario;
    private Date fecha_pedido;
    private Double total;
    private List<DetallePedidoDTO> detalles = new ArrayList<>();
    private String estado;
}
