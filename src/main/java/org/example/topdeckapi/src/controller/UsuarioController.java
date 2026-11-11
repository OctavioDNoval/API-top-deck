package org.example.topdeckapi.src.controller;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.request.UsuarioRequest;
import org.example.topdeckapi.src.DTOs.request.UsuarioUpdateRequest;
import org.example.topdeckapi.src.DTOs.response.PaginacionResponse;
import org.example.topdeckapi.src.DTOs.response.ProductoResponse;
import org.example.topdeckapi.src.DTOs.response.UsuarioResponse;
import org.example.topdeckapi.src.service.IMPL.UsuarioService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UsuarioController {
    private final UsuarioService usuarioService;


    //=======================GET========================

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> getMe(){
        return ResponseEntity.ok(usuarioService.obtenerPerfilAutenticado());
    }

    @GetMapping("/admin/obtenerPaginados")
    public ResponseEntity<PaginacionResponse<UsuarioResponse>>  getAll(
            @RequestParam(defaultValue = "1") Integer pagina,
            @RequestParam(defaultValue = "15") Integer tamanio,
            @RequestParam(defaultValue = "idPedido") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "") String filter){
        PaginacionResponse<UsuarioResponse> paginacionResponse;

        if(filter == null || filter.trim().isEmpty()){
            paginacionResponse = usuarioService.obtenerPaginados(pagina, tamanio, sortBy, direction);
        }else{
            paginacionResponse = usuarioService.obtenerPaginadosConFiltro(pagina, tamanio, sortBy, direction, filter);
        }

        return ResponseEntity.ok(paginacionResponse);
    }

    @GetMapping("/admin/{id}")
    public ResponseEntity<UsuarioResponse>  getById(@PathVariable("id") String id){
        return ResponseEntity.ok(usuarioService.obtenerPorId(id));
    }

    //=======================POST========================

    @PostMapping("/admin/crearUsuario")
    public ResponseEntity<UsuarioResponse> crearUsuario(@Valid @RequestBody UsuarioRequest request){
        return ResponseEntity.ok(usuarioService.guardar(request));
    }

    //=======================PATCH========================

    @PatchMapping("/admin/{id}")
    public ResponseEntity<UsuarioResponse>  update(@PathVariable("id") String id, @RequestBody @Valid UsuarioUpdateRequest dto){
        return ResponseEntity.ok(usuarioService.actualizarUsuario(dto, id));
    }

    //=======================DELETE========================

    @DeleteMapping("/admin/delete/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable("id") String id){
        UsuarioService.ResultadoBaja resultado = usuarioService.deleteUsuario(id);
        String message = resultado == UsuarioService.ResultadoBaja.ELIMINADO
                ? "Usuario eliminado"
                : "El usuario tenía pedidos: se anonimizó su cuenta y se conservaron los pedidos";
        return ResponseEntity.ok(Map.of("resultado", resultado.name(), "message", message));
    }
}
