package org.example.topdeckapi.src.service.IMPL;

import org.example.topdeckapi.src.DTOs.mappers.EventoMapper;
import org.example.topdeckapi.src.DTOs.request.EventoRequest;
import org.example.topdeckapi.src.DTOs.request.EventoUpdateRequest;
import org.example.topdeckapi.src.Enumerados.ESTADO_EVENTO;
import org.example.topdeckapi.src.Exception.ResourceNotFoundException;
import org.example.topdeckapi.src.Repository.IEventoRepository;
import org.example.topdeckapi.src.model.Evento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventoServiceTest {

    @Mock private IEventoRepository eventoRepository;
    @Mock private EventoMapper eventoMapper;
    @Mock private AuditService auditService;

    @InjectMocks private EventoService eventoService;

    private Evento torneo;

    @BeforeEach
    void setUp() {
        torneo = new Evento();
        torneo.setUuid("torneo-uuid");
        torneo.setNombreEvento("Torneo One Piece");
        torneo.setUbicacion("Local");
        torneo.setFecha(LocalDate.now().plusDays(10));
        torneo.setHora(LocalTime.of(18, 0));
        torneo.setPrecioEntrada(new BigDecimal("5000.00"));
        torneo.setEstado(ESTADO_EVENTO.PROXIMAMENTE);
        when(eventoRepository.findByUuid("torneo-uuid")).thenReturn(Optional.of(torneo));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void editarSoloCambiaLosCamposEnviados() {
        EventoUpdateRequest cambios = new EventoUpdateRequest();
        cambios.setPrecioEntrada(new BigDecimal("6000.00"));

        eventoService.update("torneo-uuid", cambios);

        assertThat(torneo.getPrecioEntrada()).isEqualByComparingTo("6000.00");
        assertThat(torneo.getNombreEvento()).isEqualTo("Torneo One Piece");
        assertThat(torneo.getUbicacion()).isEqualTo("Local");
        assertThat(torneo.getEstado()).isEqualTo(ESTADO_EVENTO.PROXIMAMENTE);
        verify(auditService).registrar("UPDATE", "evento", "torneo-uuid",
                "Edición del evento \"Torneo One Piece\": entrada $5.000,00 → $6.000,00");
    }

    @Test
    void cambiarLaFechaRecalculaElEstadoEnElMomento() {
        EventoUpdateRequest hoy = new EventoUpdateRequest();
        hoy.setFecha(LocalDate.now());
        eventoService.update("torneo-uuid", hoy);
        assertThat(torneo.getEstado()).isEqualTo(ESTADO_EVENTO.EN_CURSO);

        EventoUpdateRequest pasado = new EventoUpdateRequest();
        pasado.setFecha(LocalDate.now().minusDays(1));
        eventoService.update("torneo-uuid", pasado);
        assertThat(torneo.getEstado()).isEqualTo(ESTADO_EVENTO.FINALIZADO);
    }

    @Test
    void editarUnEventoInexistenteDa404() {
        assertThatThrownBy(() -> eventoService.update("nada", new EventoUpdateRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void crearUnEventoParaHoyQuedaEnCurso() {
        EventoRequest request = EventoRequest.builder()
                .nombreEvento("Prerelease")
                .ubicacion("Local")
                .fecha(LocalDate.now())
                .hora(LocalTime.of(19, 0))
                .build();

        eventoService.save(request);

        ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);
        verify(eventoRepository).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(ESTADO_EVENTO.EN_CURSO);
    }
}
