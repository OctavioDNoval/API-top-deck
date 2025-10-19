package org.example.topdeckapi.src.Exception;

public class ProductNotFoundException extends RuntimeException {
    private String msg;

    public ProductNotFoundException(String msg) {
        super(msg);
        this.msg = msg;
    }
}
