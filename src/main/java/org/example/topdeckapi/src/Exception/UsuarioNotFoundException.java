package org.example.topdeckapi.src.Exception;

public class UsuarioNotFoundException extends RuntimeException {
    private String msg;

    public UsuarioNotFoundException(String msg) {
        this.msg = msg;
    }
}
