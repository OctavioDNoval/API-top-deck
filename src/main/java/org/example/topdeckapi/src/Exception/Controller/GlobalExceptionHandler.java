package org.example.topdeckapi.src.Exception.Controller;

import org.example.topdeckapi.src.Exception.EmailYaRegistradoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/*
* Este controlador se va a encargar de recibir las
* Exception personalizadas y manejarla de forma
* de HTTP status
* */

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(EmailYaRegistradoException.class)
    public ResponseEntity<Map<String,String>> handleEmailYaRegistradoException(EmailYaRegistradoException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("message", ex.getMessage()));
    }
}
