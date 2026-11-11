package org.example.topdeckapi.src.controller;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.request.UsuarioRequest;
import org.example.topdeckapi.src.DTOs.request.UsuarioUpdateRequest;
import org.example.topdeckapi.src.DTOs.response.PaginacionResponse;
import org.example.topdeckapi.src.DTOs.response.ProductoResponse;
import org.example.topdeckapi.src.DTOs.response.UsuarioResponse;
import org.example.topdeckapi.src.DTOs.request.CambiarContrasenaRequest;
import org.example.topdeckapi.src.DTOs.request.PerfilUpdateRequest;
import org.example.topdeckapi.src.DTOs.auth.AuthResponse;
import org.example.topdeckapi.src.service.IMPL.AuthService;
import org.example.topdeckapi.src.service.IMPL.UsuarioService;
import org.springframework.security.core.Authentication;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UsuarioController {
    private final UsuarioService usuarioService;
    private final AuthService authService;


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

    @PatchMapping("/me")
    public ResponseEntity<UsuarioResponse> actualizarPerfil(@Valid @RequestBody PerfilUpdateRequest dto){
        return ResponseEntity.ok(usuarioService.actualizarPerfil(dto));
    }

    /** Closes every other session; the response carries the new token for this device. */
    @PostMapping("/me/password")
    public ResponseEntity<AuthResponse> cambiarContrasena(@Valid @RequestBody CambiarContrasenaRequest dto, Authentication authentication){
        return ResponseEntity.ok(authService.cambiarContrasena(authentication != null ? authentication.getName() : null, dto));
    }

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
