package org.example.topdeckapi.src.controller;

import lombok.RequiredArgsConstructor;

import org.example.topdeckapi.src.DTOs.request.DireccionRequest;
import org.example.topdeckapi.src.DTOs.response.DireccionResponse;
import org.example.topdeckapi.src.service.IMPL.DireccionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/direccion")
@RequiredArgsConstructor
public class DireccionController {
    private final DireccionService direccionService;

    @PostMapping("/public/save/nouser")
    public ResponseEntity<DireccionResponse> saveDireccionSinUsuario(@RequestBody DireccionRequest dto, @RequestParam String email){
        DireccionResponse d = direccionService.guardarDireccionParaGuest(dto,email);
        return ResponseEntity.ok(d);
    }

    @PostMapping("/public/save")
    public ResponseEntity<DireccionResponse> saveDireccion (@RequestBody DireccionRequest dto){
        DireccionResponse d = direccionService.guardar(dto);
        return ResponseEntity.ok(d);
    }

    @GetMapping("/user/getAll/{id}")
    public ResponseEntity<List<DireccionResponse>> getAllDireccionByUsuarioId(@PathVariable Long id){
        List<DireccionResponse> lista = direccionService.direccionesPorUsuario(id);
        return ResponseEntity.ok(lista);
    }
}
