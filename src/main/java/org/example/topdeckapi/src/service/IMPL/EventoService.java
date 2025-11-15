package org.example.topdeckapi.src.service.IMPL;

import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.CreateDTO.CreateEventoDTO;
import org.example.topdeckapi.src.DTOs.DTO.EventoDTO;
import org.example.topdeckapi.src.Enumerados.ESTADO_EVENTO;
import org.example.topdeckapi.src.Repository.IEventoRepository;
import org.example.topdeckapi.src.model.Evento;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventoService {
    private final IEventoRepository eventoRepository;

    protected EventoDTO convertToDTO(Evento evento){
        EventoDTO dto = new EventoDTO();
        dto.setIdEvento(evento.getIdEvento());
        dto.setNombreEvento(evento.getNombreEvento());
        dto.setUbicacion(evento.getUbicacion());
        dto.setFecha(evento.getFecha());
        dto.setHora(evento.getHora());
        dto.setPrecioEntrada(evento.getPrecioEntrada());
        dto.setEstado(evento.getEstado());

        return dto;
    }

    public List<EventoDTO> getAll(){
        return eventoRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Evento save (CreateEventoDTO dto){
        Evento evento = new Evento();
        evento.setNombreEvento(dto.getNombreEvento());
        evento.setUbicacion(dto.getUbicacion());
        evento.setFecha(dto.getFecha());
        evento.setHora(dto.getHora());
        evento.setPrecioEntrada(dto.getPrecioEntrada());
        evento.setEstado(ESTADO_EVENTO.PROXIMAMENTE);

        return eventoRepository.save(evento);
    }

    public boolean delete(Long id){
        if(eventoRepository.existsById(id)){
            eventoRepository.deleteById(id);
            return true;
        }else{
            return false;
        }
    }
}
