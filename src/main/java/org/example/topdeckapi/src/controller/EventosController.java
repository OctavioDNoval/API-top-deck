package org.example.topdeckapi.src.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.CreateDTO.CreateEventoDTO;
import org.example.topdeckapi.src.DTOs.DTO.EventoDTO;
import org.example.topdeckapi.src.model.Evento;
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
    public ResponseEntity<List<EventoDTO>> getAll(){
        List<EventoDTO> eventosDTO = eventosService.getAll();
        return ResponseEntity.ok(eventosDTO);
    }

    @PostMapping("/admin/save")
    @ResponseStatus(HttpStatus.CREATED)
    public Evento save(@RequestBody @Valid CreateEventoDTO dto){
        return eventosService.save(dto);
    }

    @DeleteMapping("/admin/delete/{id}")
    public ResponseEntity<EventoDTO> delete(@PathVariable Long id){
        boolean rta = eventosService.delete(id);
        return rta
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
