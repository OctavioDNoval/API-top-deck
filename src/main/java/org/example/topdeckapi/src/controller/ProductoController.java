package org.example.topdeckapi.src.controller;

import jakarta.servlet.ServletRequest;
import jakarta.validation.Valid;
import org.example.topdeckapi.src.model.Producto;
import org.example.topdeckapi.src.service.IMPL.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductoController {
    private final ProductoService productoService;

    @Autowired
    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping("/test")
    public ResponseEntity<String> test(){
        return ResponseEntity.ok("El Servicio funciona!");
    }

    @GetMapping("/public/getAll")
    public ResponseEntity<List<Producto>> getAll(){
        List<Producto> productos = productoService.findAll();
        return ResponseEntity.ok(productos);
    }

    @PostMapping("/admin/post")
    @ResponseStatus(HttpStatus.CREATED)
    public Producto post(@RequestBody@Valid Producto producto){
        return productoService.guardar(producto);
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<Producto> get(@PathVariable("id") Long id){
        return productoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/admin/edit/{id}")
    public ResponseEntity<Producto> edit(@PathVariable("id") Long id, @RequestBody @Valid Producto producto){
        return productoService.actualizarProducto(id,producto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/admin/delete/{id}")
    public ResponseEntity<Producto> delete(@PathVariable("id") Long id){
        boolean isDeleted = productoService.borrarProducto(id);
        return isDeleted
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }
}
