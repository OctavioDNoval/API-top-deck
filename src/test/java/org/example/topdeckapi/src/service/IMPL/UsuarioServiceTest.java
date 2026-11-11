package org.example.topdeckapi.src.service.IMPL;

import org.example.topdeckapi.src.DTOs.mappers.UsuarioMapper;
import org.example.topdeckapi.src.DTOs.request.UsuarioUpdateRequest;
import org.example.topdeckapi.src.Enumerados.ROL;
import org.example.topdeckapi.src.Exception.BussinesException;
import org.example.topdeckapi.src.Repository.ICarritoRepository;
import org.example.topdeckapi.src.Repository.IDireccionRepo;
import org.example.topdeckapi.src.Repository.IPedidoRepo;
import org.example.topdeckapi.src.Repository.IUsuarioRepo;
import org.example.topdeckapi.src.model.Carrito;
import org.example.topdeckapi.src.model.Direccion;
import org.example.topdeckapi.src.model.Pedido;
import org.example.topdeckapi.src.model.Usuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UsuarioServiceTest {

    @Mock private IUsuarioRepo usuarioRepo;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UsuarioMapper usuarioMapper;
    @Mock private PaginacionService paginacionService;
    @Mock private AuditService auditService;
    @Mock private ICarritoRepository carritoRepo;
    @Mock private IDireccionRepo direccionRepo;
    @Mock private IPedidoRepo pedidoRepo;

    @InjectMocks private UsuarioService usuarioService;

    private Usuario admin;
    private Usuario otroAdmin;
    private Usuario cliente;

    @BeforeEach
    void setUp() {
        admin = usuario(1L, "admin-uuid", "admin@test.com", ROL.ADMIN);
        otroAdmin = usuario(2L, "otro-uuid", "otro@test.com", ROL.ADMIN);
        cliente = usuario(3L, "cliente-uuid", "cliente@test.com", ROL.USER);
        when(usuarioRepo.findByUuid("admin-uuid")).thenReturn(Optional.of(admin));
        when(usuarioRepo.findByUuid("otro-uuid")).thenReturn(Optional.of(otroAdmin));
        when(usuarioRepo.findByUuid("cliente-uuid")).thenReturn(Optional.of(cliente));
        when(usuarioRepo.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pedidoRepo.findByUsuario_IdUsuario(anyLong())).thenReturn(List.of());
        when(direccionRepo.findByUsuario_IdUsuario(anyLong())).thenReturn(List.of());
        when(carritoRepo.findByUsuario(any())).thenReturn(Optional.empty());
        autenticarComo("admin@test.com");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void noPermiteEliminarseASiMismo() {
        when(usuarioRepo.countByRol(ROL.ADMIN)).thenReturn(2L);

        assertThatThrownBy(() -> usuarioService.deleteUsuario("admin-uuid"))
                .isInstanceOf(BussinesException.class)
                .hasMessageContaining("propia cuenta");
        verify(usuarioRepo, never()).delete(any());
    }

    @Test
    void noPermiteEliminarAlUnicoAdmin() {
        autenticarComo("alguien@test.com");
        when(usuarioRepo.countByRol(ROL.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> usuarioService.deleteUsuario("otro-uuid"))
                .isInstanceOf(BussinesException.class)
                .hasMessageContaining("único administrador");
        verify(usuarioRepo, never()).delete(any());
    }

    @Test
    void permiteEliminarOtroAdminSiQuedanAdmins() {
        when(usuarioRepo.countByRol(ROL.ADMIN)).thenReturn(2L);

        assertThat(usuarioService.deleteUsuario("otro-uuid")).isEqualTo(UsuarioService.ResultadoBaja.ELIMINADO);
        verify(usuarioRepo).delete(otroAdmin);
    }

    @Test
    void noPermiteCambiarseElPropioRol() {
        when(usuarioRepo.countByRol(ROL.ADMIN)).thenReturn(2L);

        assertThatThrownBy(() -> usuarioService.actualizarUsuario(conRol(ROL.USER), "admin-uuid"))
                .isInstanceOf(BussinesException.class)
                .hasMessageContaining("propio rol");
        verify(usuarioRepo, never()).save(any());
    }

    @Test
    void noPermiteQuitarleElRolAlUnicoAdmin() {
        autenticarComo("alguien@test.com");
        when(usuarioRepo.countByRol(ROL.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> usuarioService.actualizarUsuario(conRol(ROL.USER), "otro-uuid"))
                .isInstanceOf(BussinesException.class)
                .hasMessageContaining("único administrador");
        verify(usuarioRepo, never()).save(any());
    }

    @Test
    void permiteEditarseOtrosDatosSinTocarElRol() {
        UsuarioUpdateRequest request = new UsuarioUpdateRequest();
        request.setNombre("Nuevo nombre");
        request.setRol(ROL.ADMIN); // same role: not a change

        usuarioService.actualizarUsuario(request, "admin-uuid");

        assertThat(admin.getNombre()).isEqualTo("Nuevo nombre");
        verify(usuarioRepo).save(admin);
    }

    @Test
    void permiteAscenderAUnCliente() {
        usuarioService.actualizarUsuario(conRol(ROL.ADMIN), "cliente-uuid");

        assertThat(cliente.getRol()).isEqualTo(ROL.ADMIN);
    }

    @Test
    void sinPedidosBorraUsuarioCarritoYDirecciones() {
        Carrito carrito = new Carrito();
        Direccion casa = direccion(10L);
        when(carritoRepo.findByUsuario(cliente)).thenReturn(Optional.of(carrito));
        when(direccionRepo.findByUsuario_IdUsuario(3L)).thenReturn(List.of(casa));

        assertThat(usuarioService.deleteUsuario("cliente-uuid")).isEqualTo(UsuarioService.ResultadoBaja.ELIMINADO);

        verify(carritoRepo).delete(carrito);
        verify(direccionRepo).deleteAll(List.of(casa));
        verify(usuarioRepo).delete(cliente);
    }

    @Test
    void conPedidosAnonimizaYConservaLasDireccionesDeLosPedidos() {
        cliente.setNombre("Juan");
        cliente.setTelefono("1155554444");
        cliente.setPassword("hash");
        Direccion usada = direccion(10L);
        Direccion sinUsar = direccion(11L);
        Pedido pedido = new Pedido();
        pedido.setDireccion(usada);
        when(pedidoRepo.findByUsuario_IdUsuario(3L)).thenReturn(List.of(pedido));
        when(direccionRepo.findByUsuario_IdUsuario(3L)).thenReturn(List.of(usada, sinUsar));

        assertThat(usuarioService.deleteUsuario("cliente-uuid")).isEqualTo(UsuarioService.ResultadoBaja.ANONIMIZADO);

        verify(direccionRepo).deleteAll(List.of(sinUsar));
        verify(usuarioRepo, never()).delete(any());
        assertThat(cliente.getNombre()).isEqualTo("Usuario eliminado");
        assertThat(cliente.getEmail()).isEqualTo("eliminado-cliente-uuid@topdeck.invalid");
        assertThat(cliente.getTelefono()).isNull();
        assertThat(cliente.getPassword()).isNull();
        assertThat(cliente.getRol()).isEqualTo(ROL.GUEST);
        assertThat(cliente.getFechaBaja()).isNotNull();
        assertThat(cliente.getSesionVersionActual()).isEqualTo(1);
    }

    @Test
    void unaCuentaDadaDeBajaNoSePuedeEditarNiBorrarDeNuevo() {
        cliente.setFechaBaja(java.time.LocalDateTime.now());

        assertThatThrownBy(() -> usuarioService.deleteUsuario("cliente-uuid"))
                .isInstanceOf(org.example.topdeckapi.src.Exception.ResourceNotFoundException.class);
        assertThatThrownBy(() -> usuarioService.actualizarUsuario(conRol(ROL.ADMIN), "cliente-uuid"))
                .isInstanceOf(org.example.topdeckapi.src.Exception.ResourceNotFoundException.class);
    }

    private static Direccion direccion(Long id) {
        Direccion d = new Direccion();
        d.setIdDireccion(id);
        return d;
    }

    private static Usuario usuario(Long id, String uuid, String email, ROL rol) {
        Usuario u = new Usuario();
        u.setIdUsuario(id);
        u.setUuid(uuid);
        u.setEmail(email);
        u.setRol(rol);
        return u;
    }

    private static UsuarioUpdateRequest conRol(ROL rol) {
        UsuarioUpdateRequest request = new UsuarioUpdateRequest();
        request.setRol(rol);
        return request;
    }

    private static void autenticarComo(String email) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }
}
