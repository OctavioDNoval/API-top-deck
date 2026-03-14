package org.example.topdeckapi.src.controller;

import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.response.CarritoResponse;
import org.example.topdeckapi.src.DTOs.response.DetalleCarritoResponse;
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
    public ResponseEntity<CarritoResponse> getCarritoByUser(@PathVariable("id") Long idUsuario){
        CarritoResponse carrito = carritoService.obtenerCarritoPorUsuario(idUsuario);
        return ResponseEntity.ok(carrito);
    }

    @GetMapping("/user/{idCarrito}/detalles")
    public ResponseEntity<List<DetalleCarritoResponse>> getDetalleCarrito(@PathVariable("idCarrito") Long idCarrito){
        List<DetalleCarritoResponse> detalles = carritoService.obtenerDetalleCarrito(idCarrito);
        return ResponseEntity.ok(detalles);
    }

    @GetMapping("/public/efimero/obtenerCarrito/{sessionId}")
    public ResponseEntity<CarritoResponse> obtenerCarritoEfimero (@PathVariable String sessionId){
        return ResponseEntity.ok(carritoService.obtenerCarritoEfimero(sessionId));
    }

    @PostMapping("/user/{idCarrito}/save")
    public ResponseEntity<DetalleCarritoResponse> agregarDetalle (
            @PathVariable("idCarrito") Long idCarrito,
            @RequestParam Long idProducto,
            @RequestParam int cantidad){
        DetalleCarritoResponse detalle = carritoService.agregarAlCarrito(idProducto, idCarrito, cantidad);
        return ResponseEntity.status(HttpStatus.CREATED).body(detalle);
    }

    @PatchMapping("/user/detalle/actualizar/{idDetalle}")
    public ResponseEntity<DetalleCarritoResponse> actualizarDetalle (@PathVariable Long idDetalle,@RequestParam int nuevaCantidad){
        return ResponseEntity.ok(carritoService.actualizarCantidad(idDetalle, nuevaCantidad));
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
