package org.example.topdeckapi.src.DTOs.DTO;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetallePedidoDTO {
    private Long id_detalle_pedido;
    @NotNull
    @Positive
    private Integer cantidad;
    private Double precio_unitario;
    @NotNull
    private Long id_pedido;
    @NotNull
    private Long id_producto;
}
