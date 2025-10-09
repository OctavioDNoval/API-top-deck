package org.example.topdeckapi.src.DTOs.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetallePedidoDTO {
    private long id_detalle_pedido;
    private int cantidad;
    private double precio_unitario;
    private Integer id_pedido;
    private Integer id_Producto;
}
