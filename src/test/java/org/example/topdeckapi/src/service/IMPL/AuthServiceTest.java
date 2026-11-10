package org.example.topdeckapi.src.service.IMPL;

import org.example.topdeckapi.src.DTOs.auth.AuthResponse;
import org.example.topdeckapi.src.DTOs.auth.LoginRequest;
import org.example.topdeckapi.src.DTOs.mappers.UsuarioMapper;
import org.example.topdeckapi.src.DTOs.request.UsuarioRequest;
import org.example.topdeckapi.src.DTOs.request.VerificarCuentaRequest;
import org.example.topdeckapi.src.Enumerados.ROL;
import org.example.topdeckapi.src.Exception.BussinesException;
import org.example.topdeckapi.src.Repository.ICarritoRepository;
import org.example.topdeckapi.src.Repository.IUsuarioRepo;
import org.example.topdeckapi.src.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-0123456789";

    private IUsuarioRepo usuarioRepo;
    private ICarritoRepository carritoRepo;
    private EmailService emailService;
    private PasswordEncoder encoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        usuarioRepo = mock(IUsuarioRepo.class);
        carritoRepo = mock(ICarritoRepository.class);
        emailService = mock(EmailService.class);
        encoder = new BCryptPasswordEncoder(4);
        authService = new AuthService(usuarioRepo, encoder, new JwtService(SECRET), carritoRepo,
                mock(UsuarioMapper.class), mock(AuditService.class), emailService);
        authService.initDummyHash();
        when(usuarioRepo.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(carritoRepo.findByUsuario(any())).thenReturn(Optional.empty());
    }

    private static Usuario usuario(String email, ROL rol, String passwordHash) {
        Usuario u = new Usuario();
        u.setIdUsuario(1L);
        u.setEmail(email);
        u.setNombre("Test");
        u.setRol(rol);
        u.setPassword(passwordHash);
        return u;
    }

    private static LoginRequest login(String email, String password) {
        LoginRequest r = new LoginRequest();
        r.setEmail(email);
        r.setPassword(password);
        return r;
    }

    private static String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    // ---------- login ----------

    @Test
    void loginFailsTheSameWayForUnknownEmailAndWrongPassword() {
        when(usuarioRepo.findByEmail("nadie@example.com")).thenReturn(Optional.empty());
        when(usuarioRepo.findByEmail("user@example.com"))
                .thenReturn(Optional.of(usuario("user@example.com", ROL.USER, encoder.encode("correcta1"))));

        ResponseStatusException unknown = catchStatus(() -> authService.login(login("nadie@example.com", "x12345")));
        ResponseStatusException wrong = catchStatus(() -> authService.login(login("user@example.com", "incorrecta")));

        assertThat(unknown.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(wrong.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(unknown.getReason()).isEqualTo(wrong.getReason());
    }

    @Test
    void guestWithoutPasswordCannotLogIn() {
        when(usuarioRepo.findByEmail("guest@example.com"))
                .thenReturn(Optional.of(usuario("guest@example.com", ROL.GUEST, null)));

        assertThat(catchStatus(() -> authService.login(login("guest@example.com", "loquesea")))
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void loginWithRightPasswordReturnsToken() {
        when(usuarioRepo.findByEmail("user@example.com"))
                .thenReturn(Optional.of(usuario("user@example.com", ROL.USER, encoder.encode("correcta1"))));

        AuthResponse response = authService.login(login("user@example.com", "correcta1"));

        assertThat(response.getToken()).isNotBlank();
    }

    // ---------- guest claim ----------

    private static UsuarioRequest registro(String email) {
        UsuarioRequest r = new UsuarioRequest();
        r.setNombre("Atacante");
        r.setEmail(email);
        r.setPassword("elegida-por-otro");
        r.setTerminosAceptados(true);
        return r;
    }

    @Test
    void registeringWithGuestEmailDoesNotUpgradeAndMailsTheOwner() throws Exception {
        Usuario guest = usuario("guest@example.com", ROL.GUEST, null);
        when(usuarioRepo.findByEmail("guest@example.com")).thenReturn(Optional.of(guest));

        Optional<AuthResponse> result = authService.register(registro("guest@example.com"));

        assertThat(result).isEmpty();
        assertThat(guest.getRol()).isEqualTo(ROL.GUEST);
        assertThat(guest.getPassword()).isNull();
        assertThat(guest.getTokenVerificacionExpira()).isAfter(LocalDateTime.now().plusHours(23));

        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarVerificacionCuenta(eq(guest), token.capture());
        // Only the hash is stored, never the raw token.
        assertThat(guest.getTokenVerificacionHash()).isEqualTo(sha256(token.getValue()));
        assertThat(guest.getTokenVerificacionHash()).isNotEqualTo(token.getValue());
    }

    @Test
    void secondRegisterWithinTwoMinutesDoesNotResendTheEmail() {
        Usuario guest = usuario("guest@example.com", ROL.GUEST, null);
        when(usuarioRepo.findByEmail("guest@example.com")).thenReturn(Optional.of(guest));

        authService.register(registro("guest@example.com"));
        authService.register(registro("guest@example.com"));

        verify(emailService, org.mockito.Mockito.times(1)).enviarVerificacionCuenta(any(), anyString());
    }

    @Test
    void registeringWithAnExistingUserEmailFails() {
        when(usuarioRepo.findByEmail("user@example.com"))
                .thenReturn(Optional.of(usuario("user@example.com", ROL.USER, "hash")));

        assertThatThrownBy(() -> authService.register(registro("user@example.com")))
                .isInstanceOf(BussinesException.class);
        verify(emailService, never()).enviarVerificacionCuenta(any(), anyString());
    }

    private static VerificarCuentaRequest confirmacion(String token) {
        VerificarCuentaRequest r = new VerificarCuentaRequest();
        r.setToken(token);
        r.setPassword("nueva123");
        r.setTerminosAceptados(true);
        return r;
    }

    @Test
    void validTokenActivatesAccountWithOwnersPasswordAndIsSingleUse() throws Exception {
        Usuario guest = usuario("guest@example.com", ROL.GUEST, null);
        guest.setTokenVerificacionHash(sha256("token-ok"));
        guest.setTokenVerificacionExpira(LocalDateTime.now().plusHours(1));
        when(usuarioRepo.findByTokenVerificacionHash(sha256("token-ok"))).thenReturn(Optional.of(guest));

        AuthResponse response = authService.confirmarReclamoGuest(confirmacion("token-ok"));

        assertThat(response.getToken()).isNotBlank();
        assertThat(guest.getRol()).isEqualTo(ROL.USER);
        assertThat(encoder.matches("nueva123", guest.getPassword())).isTrue();
        assertThat(guest.getTokenVerificacionHash()).isNull();
        assertThat(guest.getTokenVerificacionExpira()).isNull();
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        Usuario guest = usuario("guest@example.com", ROL.GUEST, null);
        guest.setTokenVerificacionHash(sha256("token-viejo"));
        guest.setTokenVerificacionExpira(LocalDateTime.now().minusMinutes(1));
        when(usuarioRepo.findByTokenVerificacionHash(sha256("token-viejo"))).thenReturn(Optional.of(guest));

        assertThatThrownBy(() -> authService.confirmarReclamoGuest(confirmacion("token-viejo")))
                .isInstanceOf(BussinesException.class);
        assertThat(guest.getRol()).isEqualTo(ROL.GUEST);
    }

    @Test
    void unknownTokenIsRejected() {
        when(usuarioRepo.findByTokenVerificacionHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.confirmarReclamoGuest(confirmacion("inventado")))
                .isInstanceOf(BussinesException.class);
    }

    private static ResponseStatusException catchStatus(Runnable action) {
        try {
            action.run();
        } catch (ResponseStatusException e) {
            return e;
        }
        throw new AssertionError("Expected ResponseStatusException");
    }
}
