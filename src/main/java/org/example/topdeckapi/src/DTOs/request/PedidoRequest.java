package org.example.topdeckapi.src.DTOs.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PedidoRequest {
    @NotBlank(message = "La dirección es obligatoria")
    private String idDireccion;

    private String ipUsuario;

    @NotNull(message = "Los detalles del pedido son obligatorios")
    @NotEmpty(message = "El pedido debe tener al menos un producto")
    @Valid
    private List<DetallePedidoRequest> detalles;
}
