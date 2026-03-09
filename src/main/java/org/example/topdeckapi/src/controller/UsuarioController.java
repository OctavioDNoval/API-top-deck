package org.example.topdeckapi.src.controller;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.request.UsuarioRequest;
import org.example.topdeckapi.src.DTOs.response.PaginacionResponse;
import org.example.topdeckapi.src.DTOs.response.UsuarioResponse;
import org.example.topdeckapi.src.service.IMPL.UsuarioService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UsuarioController {
    private final UsuarioService usuarioService;


    //=======================GET========================

    @GetMapping("/admin/getAll")
    public ResponseEntity<PaginacionResponse<UsuarioResponse>>  getAll(Integer pagina, Integer tamanio, String sortBy, String direction){
        return ResponseEntity.ok(usuarioService.obtenerPaginados(pagina, tamanio, sortBy, direction));
    }

    @GetMapping("/admin/{id}")
    public ResponseEntity<UsuarioResponse>  getById(@PathVariable("id") Long id){
        return ResponseEntity.ok(usuarioService.obtenerPorId(id));
    }

    //=======================POST========================

    @PostMapping("/admin/crearUsuario")
    public ResponseEntity<UsuarioResponse> crearUsuario(@Valid @RequestBody UsuarioRequest request){
        return ResponseEntity.ok(usuarioService.guardar(request));
    }

    //=======================PATCH========================

    @PatchMapping("/admin/{id}")
    public ResponseEntity<UsuarioResponse>  update(@PathVariable("id") Long id, @RequestBody @Valid UsuarioRequest dto){
        return ResponseEntity.ok(usuarioService.actualizarUsuario(dto, id));
    }

    //=======================DELETE========================

    @DeleteMapping("/admin/delete/{id}")
    public ResponseEntity<?>  delete(@PathVariable("id") Long id){
        boolean isDeleted = usuarioService.deleteUsuario(id);
        return isDeleted
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }
}
