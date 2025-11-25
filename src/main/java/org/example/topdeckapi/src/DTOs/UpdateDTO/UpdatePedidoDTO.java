package org.example.topdeckapi.src.DTOs.UpdateDTO;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePedidoDTO {
    private Date fecha_pedido;
    @Min(value = 0)
    private Double precio;
    private String estado;
}
