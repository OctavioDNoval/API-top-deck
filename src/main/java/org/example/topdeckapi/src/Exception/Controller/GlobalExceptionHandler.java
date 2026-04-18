package org.example.topdeckapi.src.Exception.Controller;

import org.example.topdeckapi.src.DTOs.response.ApiErrorResponse;
import org.example.topdeckapi.src.Exception.BussinesException;
import org.example.topdeckapi.src.Exception.EmailYaRegistradoException;
import org.example.topdeckapi.src.Exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.Map;

/*
* Este controlador se va a encargar de recibir las
* Exception personalizadas y manejarla de forma
* de HTTP status
* */

@RestControllerAdvice
public class GlobalExceptionHandler {

    /*
    * Handler para las excepciones que tengan que ver con
    * la logica de negocio de la aplicacion
    * */
    @ExceptionHandler(BussinesException.class)
    public ResponseEntity<ApiErrorResponse> resolveBussinesException(BussinesException ex, WebRequest request) {
        ApiErrorResponse error = new ApiErrorResponse(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage(),
                request.getDescription(false).replace("uri =", ""),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    /*
    * Handler para las excepcion que tengan
    * que ver con recursos que no se encuentran en la
    * base de datos (404)
    * */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> resolveResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        ApiErrorResponse error = new ApiErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getDescription(false).replace("uri =", ""),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /*
    * Hanlder para errores internos del servidor
    * que no tiene que ver con la base de datos
    * sino un error en el servidor
    * */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> resolveException(Exception ex, WebRequest request) {
        ApiErrorResponse error = new ApiErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Ha ocurrido un error inesperado. Intente mas tarde",
                request.getDescription(false).replace("uri =", ""),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
