package org.example.topdeckapi.src.service.IMPL;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.mappers.EventoMapper;
import org.example.topdeckapi.src.DTOs.request.EventoRequest;
import org.example.topdeckapi.src.DTOs.response.EventoResponse;
import org.example.topdeckapi.src.Enumerados.ESTADO_EVENTO;
import org.example.topdeckapi.src.Repository.IEventoRepository;
import org.example.topdeckapi.src.model.Evento;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EventoService {
    private final IEventoRepository eventoRepository;
    private final EventoMapper eventoMapper;

    @Scheduled(cron = "0 0 0 * * ?")
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

    public List<EventoResponse> getAll(){
        return eventoRepository.findAll()
                .stream()
                .map(eventoMapper::toResponse)
                .collect(Collectors.toList());
    }

    public EventoResponse save (EventoRequest newEvento){
        Evento evento = new Evento();
        evento.setNombreEvento(newEvento.getNombreEvento());
        evento.setUbicacion(newEvento.getUbicacion());
        evento.setFecha(newEvento.getFecha());
        evento.setHora(newEvento.getHora());
        evento.setPrecioEntrada(newEvento.getPrecioEntrada());
        evento.setEstado(ESTADO_EVENTO.PROXIMAMENTE);

        Evento eventoGuardado = eventoRepository.save(evento);
        return eventoMapper.toResponse(eventoGuardado);
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
