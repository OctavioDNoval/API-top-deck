package org.example.topdeckapi.src.DTOs.CreateDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateDetalleCarritoDTO {
    private Long idProducto;
    private Integer cantidad;
}
