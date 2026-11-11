package org.example.topdeckapi.src.controller;

import jakarta.validation.Valid;
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
    public ResponseEntity<DireccionResponse> saveDireccion (@Valid @RequestBody DireccionRequest dto){
        DireccionResponse d = direccionService.guardar(dto);
        return ResponseEntity.ok(d);
    }

    @GetMapping("/user/getAll")
    public ResponseEntity<List<DireccionResponse>> getAllDireccionByUsuarioId(){
        log.info("Iniciando lista de direcciones");
        List<DireccionResponse> lista = direccionService.direccionesPorUsuario();
        return ResponseEntity.ok(lista);
    }

    @PatchMapping("/user/{id}")
    public ResponseEntity<DireccionResponse> update(@PathVariable("id") String id, @Valid @RequestBody DireccionRequest dto) {
        return ResponseEntity.ok(direccionService.update(dto, id));
    }

    @PatchMapping("/user/{id}/principal")
    public ResponseEntity<DireccionResponse> marcarPrincipal(@PathVariable("id") String id) {
        return ResponseEntity.ok(direccionService.marcarPrincipal(id));
    }

    @DeleteMapping("/user/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") String id) {
        direccionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
