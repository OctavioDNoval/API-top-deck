package org.example.topdeckapi.src.controller;

import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.CreateDTO.CreateDireccionDTO;
import org.example.topdeckapi.src.DTOs.DTO.DireccionDTO;
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
    public ResponseEntity<DireccionDTO> saveDireccion(@RequestBody CreateDireccionDTO dto){
        DireccionDTO direc = direccionService.guardarSinUsuario(dto);
        return ResponseEntity.ok(direc);
    }

    @GetMapping("/user/getAll/{id}")
    public ResponseEntity<List<DireccionDTO>> getAllDireccion(@PathVariable Long id){
        List<DireccionDTO> lista = direccionService.direccionesPorUsuario(id);
        return ResponseEntity.ok(lista);
    }
}
