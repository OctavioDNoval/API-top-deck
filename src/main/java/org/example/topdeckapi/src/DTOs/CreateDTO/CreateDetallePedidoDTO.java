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
    private Integer cantidad;

    @NotBlank
    @Size(min = 0)
    private Double precio_unitario;

    @NotBlank
    private Long id_pedido;

    @NotBlank
    private Long id_Producto;
}
