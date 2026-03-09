package org.example.topdeckapi.src.controller;

import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.request.CategoriaRequest;
import org.example.topdeckapi.src.DTOs.response.CategoriaResponse;

import org.example.topdeckapi.src.service.IMPL.CategoriaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/category")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CategoriaController {
    private final CategoriaService categoriaService;

    @GetMapping("/public/getAll")
    public ResponseEntity<List<CategoriaResponse>> getAll(){
        List<CategoriaResponse> categorias = categoriaService.findAll();
        return ResponseEntity.ok(categorias);
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<CategoriaResponse> getById(@PathVariable("id") Long id){
        return ResponseEntity.ok(categoriaService.buscarPorId(id));
    }

    @PatchMapping("/admin/edit/{id}")
    public ResponseEntity<CategoriaResponse> edit(@PathVariable("id") Long id, @RequestBody CategoriaRequest newCategoria){
        return ResponseEntity.ok(categoriaService.actualizar(newCategoria,id));
    }

    @PostMapping("/admin/new")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<CategoriaResponse> save(@RequestBody CategoriaRequest newCategoria){
        return ResponseEntity.ok(categoriaService.guardar(newCategoria));
    }

    @DeleteMapping("/admin/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id){
        boolean isDeleted = categoriaService.borrarCategoria(id);
        return isDeleted
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
