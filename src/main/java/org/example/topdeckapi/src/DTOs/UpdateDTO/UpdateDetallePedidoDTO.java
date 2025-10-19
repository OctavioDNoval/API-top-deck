package org.example.topdeckapi.src.DTOs.UpdateDTO;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateDetallePedidoDTO {
    @Positive
    private Integer cantidad;

}
