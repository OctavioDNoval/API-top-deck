package org.example.topdeckapi.src.controller;

import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.request.DetalleCarritoRequest;
import org.example.topdeckapi.src.DTOs.response.CarritoResponse;
import org.example.topdeckapi.src.DTOs.response.DetalleCarritoResponse;
import org.example.topdeckapi.src.model.Usuario;
import org.example.topdeckapi.src.service.IMPL.CarritoService;
import org.example.topdeckapi.src.service.IMPL.UsuarioService;
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
    private final UsuarioService usuarioService;

    @GetMapping("/user/getCarrito")
    public ResponseEntity<CarritoResponse> getCarritoByUser(){
        new Usuario();
        Usuario usuario;
        usuario = usuarioService.obtenerUsuarioAutenticado();
        CarritoResponse carrito = carritoService.obtenerCarritoPorUsuario(usuario.getIdUsuario());
        return ResponseEntity.ok(carrito);
    }

    @GetMapping("/user/{idCarrito}/detalles")
    public ResponseEntity<List<DetalleCarritoResponse>> getDetalleCarrito(@PathVariable("idCarrito") String idCarrito){
        List<DetalleCarritoResponse> detalles = carritoService.obtenerDetalleCarrito(idCarrito);
        return ResponseEntity.ok(detalles);
    }

    // LEGACY: Endpoints efímeros — movidos a localStorage en el frontend
    // @GetMapping("/public/efimero/obtenerCarrito/{sessionId}")
    // public ResponseEntity<CarritoResponse> obtenerCarritoEfimero (@PathVariable String sessionId){
    //     return ResponseEntity.ok(carritoService.obtenerCarritoEfimero(sessionId));
    // }
    //
    // @PostMapping("/public/efimero/agregarDetalle")
    // public ResponseEntity<DetalleCarritoResponse> agregarDetalleEfimero( @RequestBody DetalleCarritoRequest detalleCarritoRequest){
    //     return ResponseEntity.ok(carritoService.agregarDetalleCarritoEfimero(detalleCarritoRequest));
    // }

    @PostMapping("/user/agregarDetalle")
    public ResponseEntity<DetalleCarritoResponse> agregarDetalle (@RequestBody DetalleCarritoRequest detalleCarritoRequest){
        DetalleCarritoResponse detalle = carritoService.agregarAlCarrito(detalleCarritoRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(detalle);
    }

    @PatchMapping("/user/detalle/actualizar/{idDetalle}")
    public ResponseEntity<DetalleCarritoResponse> actualizarDetalle (@PathVariable String idDetalle,@RequestParam int nuevaCantidad){
        return ResponseEntity.ok(carritoService.actualizarCantidad(idDetalle, nuevaCantidad));
    }
    @DeleteMapping("/user/detalle/{idDetalle}")
    public ResponseEntity<Void> eliminarProducto(@PathVariable("idDetalle") String idDetalle){
        boolean isDeleted = carritoService.deleteProducto(idDetalle);
        if(isDeleted){
            return ResponseEntity.noContent().build();
        }else{
            return ResponseEntity.notFound().build();
        }
    }

    // LEGACY: Endpoint efímero — movido a localStorage en el frontend
    // @DeleteMapping("/public/efimero/{idDetalleCarrito}/eliminarDetalle")
    // public ResponseEntity<Void> eliminarEfimero( @PathVariable("idDetalleCarrito") String idDetalleCarrito){
    //     boolean isDeleted = carritoService.eliminarDeCarritoEfimero(idDetalleCarrito);
    //     if(isDeleted){
    //         return ResponseEntity.noContent().build();
    //     }else{
    //         return ResponseEntity.notFound().build();
    //     }
    // }

    @DeleteMapping("/user/{idCarrito}/empty")
    public ResponseEntity<Void> vaciarCarrito(@PathVariable("idCarrito") String idCarrito){
        carritoService.borrarCarrito(idCarrito);
        return ResponseEntity.noContent().build();
    }

    // LEGACY: Merge efímero — ya no se usa, la transferencia se hace desde el frontend
    // @PostMapping("/user/mergeEfimero/{sessionId}")
    // public ResponseEntity<CarritoResponse> mergeEfimero(@PathVariable String sessionId){
    //     Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
    //     CarritoResponse merged = carritoService.mergeCarritoEfimeroToUser(sessionId, usuario.getIdUsuario());
    //     return ResponseEntity.ok(merged);
    // }

}
