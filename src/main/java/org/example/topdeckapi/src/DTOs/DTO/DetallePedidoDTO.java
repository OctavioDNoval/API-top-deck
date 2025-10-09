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
    private long id_pedido;
    private long id_Producto;
}
