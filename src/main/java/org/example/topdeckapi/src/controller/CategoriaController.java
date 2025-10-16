package org.example.topdeckapi.src.controller;

import org.example.topdeckapi.src.model.Categoria;
import org.example.topdeckapi.src.service.IMPL.CategoriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/category")
public class CategoriaController {
    private final CategoriaService categoriaService;

    @Autowired
    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping("/public/getAll")
    public ResponseEntity<List<Categoria>> getAll(){
        List<Categoria> categorias = categoriaService.findAll();
        return ResponseEntity.ok(categorias);
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<Categoria> getById(@PathVariable("id") Long id){
        return categoriaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/admin/edit/{id}")
    public ResponseEntity<Categoria> edit(@PathVariable("id") Long id, @RequestBody Categoria newCategoria){
        return categoriaService.actualizar(newCategoria,id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/admin/new")
    @ResponseStatus(HttpStatus.CREATED)
    public Categoria save(@RequestBody Categoria newCategoria){
        return categoriaService.guardar(newCategoria);
    }

    @DeleteMapping("/admin/delete/{id}")
    public ResponseEntity<Categoria> delete(@PathVariable("id") Long id){
        boolean isDeleted = categoriaService.borrarCategoria(id);
        return isDeleted
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
