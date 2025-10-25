package org.example.topdeckapi.src.controller;

import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.model.Carrito;
import org.example.topdeckapi.src.service.IMPL.CarritoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/carrito")
public class CarritoController {
    private final CarritoService carritoService;

    @GetMapping("/public/usuario/{id}")
    public ResponseEntity<Carrito> getCarritoByUser(@PathVariable("id") Long idUsuario){
        Carrito carrito = carritoService.obtenerCarritoPorUsuario(idUsuario);
        return ResponseEntity.ok(carrito);
    }

    
}
