package org.example.topdeckapi.src.DTOs.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DetalleCarritoRequest {
    private Long idCarrito;
    private Long idProducto;
    private Integer cantidad;
}
