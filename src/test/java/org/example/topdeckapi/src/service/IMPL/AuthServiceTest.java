package org.example.topdeckapi.src.service.IMPL;

import org.example.topdeckapi.src.DTOs.auth.AuthResponse;
import org.example.topdeckapi.src.DTOs.auth.LoginRequest;
import org.example.topdeckapi.src.DTOs.mappers.UsuarioMapper;
import org.example.topdeckapi.src.DTOs.request.RestablecerContrasenaRequest;
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
import java.time.Duration;
import java.time.Instant;
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
    private JwtService jwtService;
    private UsuarioMapper usuarioMapper;

    @BeforeEach
    void setUp() {
        usuarioRepo = mock(IUsuarioRepo.class);
        carritoRepo = mock(ICarritoRepository.class);
        emailService = mock(EmailService.class);
        encoder = new BCryptPasswordEncoder(4);
        jwtService = new JwtService(SECRET);
        usuarioMapper = mock(UsuarioMapper.class);
        authService = new AuthService(usuarioRepo, encoder, jwtService, carritoRepo,
                usuarioMapper, mock(AuditService.class), emailService, new LoginAttemptService());
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

        authService.register(registro("guest@example.com"));

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
    void registeringAnExistingAccountRevealsNothingAndOnlyNotifiesTheOwner() {
        Usuario existente = usuario("user@example.com", ROL.USER, "hash-original");
        when(usuarioRepo.findByEmail("user@example.com")).thenReturn(Optional.of(existente));

        authService.register(registro("user@example.com")); // no exception: same answer as a new email

        assertThat(existente.getPassword()).isEqualTo("hash-original");
        verify(emailService).enviarAvisoCuentaExistente(existente);
        verify(emailService, never()).enviarConfirmacionRegistro(any(), anyString());
    }

    // ---------- registration with email confirmation ----------

    @Test
    void newRegistrationIsUnconfirmedAndMailsAConfirmationLink() throws Exception {
        when(usuarioRepo.findByEmail("nuevo@example.com")).thenReturn(Optional.empty());
        when(usuarioMapper.toEntity(any(UsuarioRequest.class))).thenAnswer(inv -> {
            Usuario u = new Usuario();
            u.setEmail("nuevo@example.com");
            return u;
        });

        authService.register(registro("nuevo@example.com"));

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarConfirmacionRegistro(guardado.capture(), token.capture());
        Usuario u = guardado.getValue();
        assertThat(u.getRol()).isEqualTo(ROL.USER);
        assertThat(u.getEmailVerificado()).isFalse();
        assertThat(u.getTokenVerificacionHash()).isEqualTo(sha256(token.getValue()));
    }

    @Test
    void unconfirmedAccountCannotLogInAndGetsTheLinkAgain() {
        Usuario u = usuario("nuevo@example.com", ROL.USER, encoder.encode("correcta1"));
        u.setEmailVerificado(false);
        when(usuarioRepo.findByEmail("nuevo@example.com")).thenReturn(Optional.of(u));

        assertThat(catchStatus(() -> authService.login(login("nuevo@example.com", "correcta1")))
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(emailService).enviarConfirmacionRegistro(eq(u), anyString());
    }

    @Test
    void reRegisteringAnUnconfirmedAccountDoesNotChangeItsPassword() {
        Usuario u = usuario("victima@example.com", ROL.USER, "hash-de-la-victima");
        u.setEmailVerificado(false);
        when(usuarioRepo.findByEmail("victima@example.com")).thenReturn(Optional.of(u));

        authService.register(registro("victima@example.com"));

        assertThat(u.getPassword()).isEqualTo("hash-de-la-victima");
        assertThat(u.getNombre()).isEqualTo("Test");
    }

    @Test
    void confirmingNeedsThePasswordChosenAtRegistration() throws Exception {
        // Someone else registered this email; the real owner opens the link but
        // does not know that password, so the stranger's account is not confirmed.
        Usuario u = usuario("victima@example.com", ROL.USER, encoder.encode("del-atacante"));
        u.setEmailVerificado(false);
        u.setTokenVerificacionHash(sha256("link"));
        u.setTokenVerificacionExpira(LocalDateTime.now().plusHours(1));
        when(usuarioRepo.findByTokenVerificacionHash(sha256("link"))).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> authService.confirmarEmail("link", "la-mia-123"))
                .isInstanceOf(BussinesException.class);
        assertThat(u.getEmailVerificado()).isFalse();

        AuthResponse ok = authService.confirmarEmail("link", "del-atacante");
        assertThat(ok.getToken()).isNotBlank();
        assertThat(u.getEmailVerificado()).isTrue();
        assertThat(u.getTokenVerificacionHash()).isNull();
    }

    // ---------- brute force ----------

    @Test
    void fiveWrongPasswordsLockTheAccountEvenForTheRightOne() {
        when(usuarioRepo.findByEmail("user@example.com"))
                .thenReturn(Optional.of(usuario("user@example.com", ROL.USER, encoder.encode("correcta1"))));
        for (int i = 0; i < 5; i++) {
            catchStatus(() -> authService.login(login("user@example.com", "mala")));
        }

        assertThat(catchStatus(() -> authService.login(login("user@example.com", "correcta1")))
                .getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    // ---------- sessions ----------

    @Test
    void refreshKeepsTheLoginTimeAndStopsAfterLogout() {
        Usuario u = usuario("user@example.com", ROL.USER, "hash");
        u.setSesionVersion(0);
        when(usuarioRepo.findByEmail("user@example.com")).thenReturn(Optional.of(u));
        Instant login = Instant.now().minus(Duration.ofDays(2));
        String token = jwtService.generarToken(u, login);

        String renovado = authService.refreshToken(token).getToken();
        assertThat(jwtService.authTime(jwtService.leerClaims(renovado)).getEpochSecond()).isEqualTo(login.getEpochSecond());

        authService.logout("user@example.com");
        assertThat(catchStatus(() -> authService.refreshToken(renovado)).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refreshIsRefusedPastSevenDays() {
        Usuario u = usuario("user@example.com", ROL.USER, "hash");
        when(usuarioRepo.findByEmail("user@example.com")).thenReturn(Optional.of(u));
        String viejo = jwtService.generarToken(u, Instant.now().minus(Duration.ofDays(8)));

        assertThat(catchStatus(() -> authService.refreshToken(viejo)).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ---------- password reset ----------

    @Test
    void forgotPasswordForUnknownEmailSendsNothing() {
        when(usuarioRepo.findByEmail("nadie@example.com")).thenReturn(Optional.empty());

        authService.olvideContrasena("nadie@example.com");

        verify(emailService, never()).enviarRestablecerContrasena(any(), anyString());
    }

    @Test
    void resetLinkSetsPasswordOnceAndClosesOtherSessions() throws Exception {
        Usuario u = usuario("user@example.com", ROL.USER, encoder.encode("vieja1234"));
        u.setSesionVersion(3);
        when(usuarioRepo.findByEmail("user@example.com")).thenReturn(Optional.of(u));

        authService.olvideContrasena("user@example.com");
        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarRestablecerContrasena(eq(u), token.capture());
        assertThat(u.getTokenResetHash()).isEqualTo(sha256(token.getValue()));
        when(usuarioRepo.findByTokenResetHash(sha256(token.getValue()))).thenReturn(Optional.of(u));

        RestablecerContrasenaRequest req = new RestablecerContrasenaRequest(token.getValue(), "nueva-clave-1");
        authService.restablecerContrasena(req);

        assertThat(encoder.matches("nueva-clave-1", u.getPassword())).isTrue();
        assertThat(u.getSesionVersion()).isEqualTo(4);
        assertThat(u.getTokenResetHash()).isNull();
        when(usuarioRepo.findByTokenResetHash(sha256(token.getValue()))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.restablecerContrasena(req)).isInstanceOf(BussinesException.class);
    }

    private static VerificarCuentaRequest confirmacion(String token) {
        VerificarCuentaRequest r = new VerificarCuentaRequest();
        r.setToken(token);
        r.setPassword("nueva1234");
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
        assertThat(encoder.matches("nueva1234", guest.getPassword())).isTrue();
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
