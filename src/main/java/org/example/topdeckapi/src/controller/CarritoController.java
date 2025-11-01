package org.example.topdeckapi.src.controller;

import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.DTO.DetalleCarritoDTO;
import org.example.topdeckapi.src.model.Carrito;
import org.example.topdeckapi.src.model.DetalleCarrito;
import org.example.topdeckapi.src.service.IMPL.CarritoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/carrito")
public class CarritoController {
    private final CarritoService carritoService;

    @GetMapping("/usuario/{id}")
    public ResponseEntity<Carrito> getCarritoByUser(@PathVariable("id") Long idUsuario){
        Carrito carrito = carritoService.obtenerCarritoPorUsuario(idUsuario);
        return ResponseEntity.ok(carrito);
    }

    @GetMapping("/user/{idCarrito}/detalles")
    public ResponseEntity<List<DetalleCarritoDTO>> getDetalleCarrito(@PathVariable("idCarrito") Long idCarrito){
        List<DetalleCarritoDTO> detalles = carritoService.obtenerDetalleCarrito(idCarrito);
        return ResponseEntity.ok(detalles);
    }

    @PostMapping("/user/{idCarrito}/save")
    public ResponseEntity<DetalleCarritoDTO> agregarDetalle (
            @PathVariable("idCarrito") Long idCarrito,
            @RequestParam Long idProducto,
            @RequestParam int cantidad){
        DetalleCarritoDTO detalle = carritoService.agregarAlCarrito(idProducto, idCarrito, cantidad);
        return ResponseEntity.status(HttpStatus.CREATED).body(detalle);
    }

    @DeleteMapping("/user/detalle/{idDetalle}")
    public ResponseEntity<Void> eliminarProducto(@PathVariable("idDetalle") Long idDetalle){
        boolean isDeleted = carritoService.deleteProducto(idDetalle);
        if(isDeleted){
            return ResponseEntity.noContent().build();
        }else{
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/user/{idCarrito}/empty")
    public ResponseEntity<Void> vaciarCarrito(@PathVariable("idCarrito") Long idCarrito){
        carritoService.borrarCarrito(idCarrito);
        return ResponseEntity.noContent().build();
    }

}
