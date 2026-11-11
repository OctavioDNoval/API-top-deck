package org.example.topdeckapi.src.service.IMPL;

import org.example.topdeckapi.src.DTOs.mappers.UsuarioMapper;
import org.example.topdeckapi.src.DTOs.request.UsuarioUpdateRequest;
import org.example.topdeckapi.src.Enumerados.ROL;
import org.example.topdeckapi.src.Exception.BussinesException;
import org.example.topdeckapi.src.Repository.IUsuarioRepo;
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

        assertThat(usuarioService.deleteUsuario("otro-uuid")).isTrue();
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
