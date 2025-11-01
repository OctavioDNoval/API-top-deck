package org.example.topdeckapi.src.DTOs.DTO;

import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DetalleCarritoDTO {
    private Long id_DetalleCarrito;
    private Integer cantidad;
    private ProductoDTO productoDTO;
    private Long id_carrito;
}
