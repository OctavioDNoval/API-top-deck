package org.example.topdeckapi.src.service.IMPL;

import org.example.topdeckapi.src.util.AuditMensaje;
import org.example.topdeckapi.src.DTOs.request.EventoUpdateRequest;
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
    private final AuditService auditService;

    @Scheduled(cron = "0 0 0 * * ?")
    public void actualizarEstadoEventoAutomaticamente (){
        LocalDate hoy =  LocalDate.now();
        List<Evento> eventos = eventoRepository.findAll();

        for (Evento evento : eventos) {
            ESTADO_EVENTO nuevoEstado = calcularEstadoEvento( evento.getFecha(), hoy);

            if(evento.getEstado() != nuevoEstado){
                ESTADO_EVENTO estadoAnterior = evento.getEstado();
                evento.setEstado(nuevoEstado);
                eventoRepository.save(evento);
                auditService.registrar("UPDATE", "evento", evento.getUuid(),
                        "Evento \"" + evento.getNombreEvento() + "\": " + estadoAnterior + " → " + nuevoEstado + " (automático)");
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

    // 'del evento "Torneo" — 20/12/2026 18:00 h, Local, entrada $5.000,00'
    private static String describir(Evento e) {
        boolean gratis = e.getPrecioEntrada() == null || e.getPrecioEntrada().signum() == 0;
        return "del evento \"" + e.getNombreEvento() + "\" — " + AuditMensaje.fecha(e.getFecha())
                + (e.getHora() != null ? " " + AuditMensaje.hora(e.getHora()) : "")
                + ", " + e.getUbicacion() + ", entrada " + (gratis ? "gratis" : AuditMensaje.dinero(e.getPrecioEntrada()));
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
        evento.setEstado(calcularEstadoEvento(newEvento.getFecha(), LocalDate.now()));

        Evento eventoGuardado = eventoRepository.save(evento);
        auditService.registrar("INSERT", "evento", eventoGuardado.getUuid(), "Creación " + describir(eventoGuardado));
        return eventoMapper.toResponse(eventoGuardado);
    }

    public EventoResponse update(String uuid, EventoUpdateRequest cambios) {
        Evento evento = eventoRepository.findByUuid(uuid)
                .orElseThrow(() -> new org.example.topdeckapi.src.Exception.ResourceNotFoundException("Evento no encontrado"));
        String nombreAntes = evento.getNombreEvento();
        String ubicacionAntes = evento.getUbicacion();
        LocalDate fechaAntes = evento.getFecha();
        java.time.LocalTime horaAntes = evento.getHora();
        java.math.BigDecimal precioAntes = evento.getPrecioEntrada();
        ESTADO_EVENTO estadoAntes = evento.getEstado();

        if (cambios.getNombreEvento() != null) evento.setNombreEvento(cambios.getNombreEvento().trim());
        if (cambios.getUbicacion() != null) evento.setUbicacion(cambios.getUbicacion().trim());
        if (cambios.getHora() != null) evento.setHora(cambios.getHora());
        if (cambios.getPrecioEntrada() != null) evento.setPrecioEntrada(cambios.getPrecioEntrada());
        if (cambios.getFecha() != null) {
            evento.setFecha(cambios.getFecha());
            // Don't wait for the nightly job: the state follows the new date right away.
            evento.setEstado(calcularEstadoEvento(cambios.getFecha(), LocalDate.now()));
        }
        Evento guardado = eventoRepository.save(evento);
        AuditMensaje.Cambios c = new AuditMensaje.Cambios()
                .valor("nombre", nombreAntes, guardado.getNombreEvento())
                .valor("ubicación", ubicacionAntes, guardado.getUbicacion())
                .valor("fecha", fechaAntes, guardado.getFecha())
                .valor("hora", horaAntes, guardado.getHora())
                .monto("entrada", precioAntes, guardado.getPrecioEntrada())
                .valor("estado", estadoAntes, guardado.getEstado());
        auditService.registrar("UPDATE", "evento", guardado.getUuid(), c.texto("Edición del evento \"" + nombreAntes + "\""));
        return eventoMapper.toResponse(guardado);
    }

    public boolean delete(String uuid){
        Evento evento = eventoRepository.findByUuid(uuid)
                .orElseThrow(() -> new org.example.topdeckapi.src.Exception.ResourceNotFoundException("Evento no encontrado"));
        auditService.registrar("DELETE", "evento", evento.getUuid(), "Eliminación " + describir(evento));
        eventoRepository.delete(evento);
        return true;
    }
}
