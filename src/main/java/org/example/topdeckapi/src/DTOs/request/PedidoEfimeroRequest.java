package org.example.topdeckapi.src.DTOs.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PedidoEfimeroRequest {
    private UsuarioRequest usuario;
    private DireccionRequest direccion;
    private String ipUsuario;
    private List<DetallePedidoRequest> detalles;
}
