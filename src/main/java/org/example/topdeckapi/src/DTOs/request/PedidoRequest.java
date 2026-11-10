package org.example.topdeckapi.src.DTOs.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.example.topdeckapi.src.Enumerados.TIPO_ENTREGA;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PedidoRequest {
    // Required only for ENVIO (checked in the service).
    private String idDireccion;

    @NotNull(message = "Elegí envío a domicilio o retiro en el local")
    private TIPO_ENTREGA tipoEntrega;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^[+]?[0-9 ()-]{8,20}$", message = "Ingresá un teléfono válido (solo números, espacios, guiones o +)")
    private String telefonoContacto;

    private String ipUsuario;

    @NotNull(message = "Los detalles del pedido son obligatorios")
    @NotEmpty(message = "El pedido debe tener al menos un producto")
    @Valid
    private List<DetallePedidoRequest> detalles;
}
