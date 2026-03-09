package org.example.topdeckapi.src.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.request.EventoRequest;
import org.example.topdeckapi.src.DTOs.response.EventoResponse;
import org.example.topdeckapi.src.service.IMPL.EventoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/eventos")
@RequiredArgsConstructor
public class EventosController {
    private final EventoService eventosService;

    @GetMapping("/public/getAll")
    public ResponseEntity<List<EventoResponse>> getAll(){
        List<EventoResponse> eventosDTO = eventosService.getAll();
        return ResponseEntity.ok(eventosDTO);
    }

    @PostMapping("/admin/save")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<EventoResponse> save(@RequestBody @Valid EventoRequest dto){
        return ResponseEntity.ok(eventosService.save(dto));
    }

    @DeleteMapping("/admin/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id){
        boolean rta = eventosService.delete(id);
        return rta
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
