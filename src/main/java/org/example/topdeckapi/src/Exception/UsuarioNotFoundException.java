package org.example.topdeckapi.src.Exception;

public class UsuarioNotFoundException extends ResourceNotFoundException {
    public UsuarioNotFoundException(String message) {
        super(message);
    }
}
