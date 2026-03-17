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

    @PostMapping("/user/save")
    public ResponseEntity<DireccionResponse> saveDireccion (@RequestBody DireccionRequest dto){
        DireccionResponse d = direccionService.guardar(dto);
        return ResponseEntity.ok(d);
    }

    @GetMapping("/user/getAll/")
    public ResponseEntity<List<DireccionResponse>> getAllDireccionByUsuarioId(){
        List<DireccionResponse> lista = direccionService.direccionesPorUsuario();
        return ResponseEntity.ok(lista);
    }
}
