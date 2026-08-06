package org.example.topdeckapi.src.controller;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.example.topdeckapi.src.DTOs.request.DireccionRequest;
import org.example.topdeckapi.src.DTOs.response.DireccionResponse;
import org.example.topdeckapi.src.service.IMPL.DireccionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/direccion")
@RequiredArgsConstructor
@Slf4j

public class DireccionController {
    private final DireccionService direccionService;

    @PostMapping("/user/save")
    public ResponseEntity<DireccionResponse> saveDireccion (@RequestBody DireccionRequest dto){
        DireccionResponse d = direccionService.guardar(dto);
        return ResponseEntity.ok(d);
    }

    @GetMapping("/user/getAll")
    public ResponseEntity<List<DireccionResponse>> getAllDireccionByUsuarioId(){
        log.info("Iniciando lista de direcciones");
        List<DireccionResponse> lista = direccionService.direccionesPorUsuario();
        log.info("Lista de direcciones: {}", lista);
        return ResponseEntity.ok(lista);
    }
}
