package org.example.topdeckapi.src.service.IMPL;

import org.example.topdeckapi.src.DTOs.mappers.DireccionMapper;
import org.example.topdeckapi.src.DTOs.request.DireccionRequest;
import org.example.topdeckapi.src.Exception.ResourceNotFoundException;
import org.example.topdeckapi.src.Repository.IDireccionRepo;
import org.example.topdeckapi.src.Repository.IPedidoRepo;
import org.example.topdeckapi.src.Repository.IUsuarioRepo;
import org.example.topdeckapi.src.model.Direccion;
import org.example.topdeckapi.src.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DireccionServiceTest {

    @Mock private IDireccionRepo direccionRepo;
    @Mock private DireccionMapper direccionMapper;
    @Mock private IUsuarioRepo usuarioRepo;
    @Mock private UsuarioService usuarioService;
    @Mock private AuditService auditService;
    @Mock private IPedidoRepo pedidoRepo;

    @InjectMocks private DireccionService direccionService;

    private Usuario ana;
    private Usuario beto;
    private Direccion casa;
    private Direccion trabajo;
    private final List<Direccion> activasDeAna = new ArrayList<>();

    @BeforeEach
    void setUp() {
        ana = usuario(1L);
        beto = usuario(2L);
        casa = direccion(10L, "casa-uuid", ana, true);
        trabajo = direccion(11L, "trabajo-uuid", ana, false);
        activasDeAna.add(casa);
        activasDeAna.add(trabajo);

        when(direccionRepo.findByUuid("casa-uuid")).thenReturn(Optional.of(casa));
        when(direccionRepo.findByUuid("trabajo-uuid")).thenReturn(Optional.of(trabajo));
        when(direccionRepo.findByUsuario_IdUsuarioAndActivaTrue(1L)).thenAnswer(inv ->
                activasDeAna.stream().filter(d -> Boolean.TRUE.equals(d.getActiva())).toList());
        when(direccionRepo.save(any(Direccion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(ana);
    }

    @Test
    void otroUsuarioNoPuedeEditarBorrarNiMarcarPrincipal() {
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(beto);

        assertThatThrownBy(() -> direccionService.delete("casa-uuid")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> direccionService.update(request(), "casa-uuid")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> direccionService.marcarPrincipal("trabajo-uuid")).isInstanceOf(AccessDeniedException.class);
        verify(direccionRepo, never()).delete(any());
        verify(direccionRepo, never()).save(any());
    }

    @Test
    void borrarSinPedidosLaEliminaYPasaLaPrincipalALaSiguiente() {
        when(pedidoRepo.existsByDireccion_IdDireccion(10L)).thenReturn(false);

        direccionService.delete("casa-uuid");

        verify(direccionRepo).delete(casa);
        assertThat(trabajo.getPrincipal()).isTrue();
    }

    @Test
    void borrarConPedidosSoloLaOculta() {
        when(pedidoRepo.existsByDireccion_IdDireccion(10L)).thenReturn(true);

        direccionService.delete("casa-uuid");

        verify(direccionRepo, never()).delete(any());
        assertThat(casa.getActiva()).isFalse();
        assertThat(casa.getPrincipal()).isFalse();
    }

    @Test
    void unaDireccionOcultaNoSePuedeTocar() {
        casa.setActiva(false);

        assertThatThrownBy(() -> direccionService.delete("casa-uuid")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void editarSinPedidosCambiaLaMismaDireccion() {
        when(pedidoRepo.existsByDireccion_IdDireccion(11L)).thenReturn(false);

        direccionService.update(request(), "trabajo-uuid");

        assertThat(trabajo.getDireccion()).isEqualTo("Calle Nueva");
        assertThat(trabajo.getActiva()).isTrue();
    }

    @Test
    void editarConPedidosConservaLaViejaYCreaOtra() {
        when(pedidoRepo.existsByDireccion_IdDireccion(10L)).thenReturn(true);

        direccionService.update(request(), "casa-uuid");

        assertThat(casa.getActiva()).isFalse();
        assertThat(casa.getDireccion()).isEqualTo("Calle Vieja"); // the orders keep the original
        ArgumentCaptor<Direccion> captor = ArgumentCaptor.forClass(Direccion.class);
        verify(direccionRepo, atLeastOnce()).save(captor.capture());
        Direccion nueva = captor.getValue();
        assertThat(nueva).isNotSameAs(casa);
        assertThat(nueva.getDireccion()).isEqualTo("Calle Nueva");
        assertThat(nueva.getUsuario()).isSameAs(ana);
        assertThat(nueva.getPrincipal()).isTrue(); // it was the main one
    }

    @Test
    void marcarPrincipalDesmarcaLasDemas() {
        direccionService.marcarPrincipal("trabajo-uuid");

        assertThat(trabajo.getPrincipal()).isTrue();
        assertThat(casa.getPrincipal()).isFalse();
    }

    private static Usuario usuario(Long id) {
        Usuario u = new Usuario();
        u.setIdUsuario(id);
        return u;
    }

    private static Direccion direccion(Long id, String uuid, Usuario usuario, boolean principal) {
        Direccion d = Direccion.builder()
                .usuario(usuario)
                .direccion("Calle Vieja")
                .altura("100")
                .ciudad("Mar del Plata")
                .provincia("Buenos Aires")
                .codigoPostal("7600")
                .principal(principal)
                .build();
        d.setIdDireccion(id);
        d.setUuid(uuid);
        return d;
    }

    private static DireccionRequest request() {
        return DireccionRequest.builder()
                .direccion("Calle Nueva")
                .altura("200")
                .ciudad("Mar del Plata")
                .provincia("Buenos Aires")
                .codigoPostal("7600")
                .build();
    }
}
