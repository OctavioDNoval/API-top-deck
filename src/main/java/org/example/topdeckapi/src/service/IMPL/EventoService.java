package org.example.topdeckapi.src.service.IMPL;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.CreateDTO.CreateEventoDTO;
import org.example.topdeckapi.src.DTOs.DTO.EventoDTO;
import org.example.topdeckapi.src.Enumerados.ESTADO_EVENTO;
import org.example.topdeckapi.src.Repository.IEventoRepository;
import org.example.topdeckapi.src.model.Evento;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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
        dto.setFecha(evento.getFecha().toString());
        dto.setHora(evento.getHora().toString());
        dto.setPrecioEntrada(evento.getPrecioEntrada());
        dto.setEstado(evento.getEstado());

        return dto;
    }

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void actualizarEstadoEventoAutomaticamente (){
        LocalDate hoy =  LocalDate.now();
        List<Evento> eventos = eventoRepository.findAll();

        for (Evento evento : eventos) {
            ESTADO_EVENTO nuevoEstado = calcularEstadoEvento( evento.getFecha(), hoy);

            if(evento.getEstado() != nuevoEstado){
                evento.setEstado(nuevoEstado);
                eventoRepository.save(evento);
                System.out.println("EVENTO ACTUALIZADO: " + evento.getIdEvento() + nuevoEstado);
            }
        }
    }

    private ESTADO_EVENTO calcularEstadoEvento(LocalDate fechaEvento, LocalDate hoy){
        if(fechaEvento.isBefore(hoy)){
            return ESTADO_EVENTO.FINALIZADO;
        } else if (fechaEvento.isEqual(hoy)) {
            return ESTADO_EVENTO.EN_CURSO;
        }else {
            return ESTADO_EVENTO.PROXIMAMENTE;
        }
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
