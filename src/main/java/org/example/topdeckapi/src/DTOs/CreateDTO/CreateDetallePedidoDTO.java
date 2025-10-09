package org.example.topdeckapi.src.DTOs.CreateDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.antlr.v4.runtime.misc.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDetallePedidoDTO {
    @NotBlank
    @Size(min = 0)
    private int cantidad;

    @NotBlank
    @Size(min = 0)
    private double precio_unitario;

    @NotBlank
    private long id_pedido;

    @NotBlank
    private long id_Producto;
}
