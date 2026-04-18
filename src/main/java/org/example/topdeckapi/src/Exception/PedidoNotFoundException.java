package org.example.topdeckapi.src.Exception;

public class PedidoNotFoundException extends ResourceNotFoundException {
    public PedidoNotFoundException(String message) {
        super(message);
    }
}
