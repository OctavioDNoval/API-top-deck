package org.example.topdeckapi.src.DTOs.CreateDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreatePedidoDTO {
    private Long id_usuario;
    @NotBlank
    private Date fecha_pedido;

    @Size(min = 0)
    private double precio;

    private List<CreateDetallePedidoDTO> detalles = new ArrayList<>();
}
