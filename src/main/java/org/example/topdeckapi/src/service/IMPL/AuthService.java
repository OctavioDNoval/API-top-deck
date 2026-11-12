package org.example.topdeckapi.src.service.IMPL;

import org.example.topdeckapi.src.DTOs.request.CambiarContrasenaRequest;
import org.example.topdeckapi.src.DTOs.request.CambiarEmailRequest;
import io.jsonwebtoken.Claims;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.example.topdeckapi.src.model.Carrito;
import org.example.topdeckapi.src.model.Usuario;
import org.example.topdeckapi.src.util.FechaUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Account lifecycle: registration with email confirmation, login with per-account
 * lockout, session renewal capped at 7 days, logout that revokes every session,
 * password reset and the GUEST claim flow.
 *
 * Registration and "forgot password" answer the same way whether the email exists
 * or not, so neither can be used to find out who has an account.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {

    private final IUsuarioRepo usuarioRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final ICarritoRepository carritoRepo;
    private final UsuarioMapper usuarioMapper;
    private final AuditService auditService;
    private final EmailService emailService;
    private final LoginAttemptService loginAttemptService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    static final Duration VALIDEZ_TOKEN_VERIFICACION = Duration.ofHours(24);
    static final Duration VALIDEZ_TOKEN_RESET = Duration.ofHours(1);
    static final Duration VALIDEZ_TOKEN_EMAIL = Duration.ofHours(24);
    public static final String MENSAJE_CAMBIO_EMAIL =
            "Te enviamos un link al email nuevo. Abrilo para confirmar el cambio (vence en 24 horas).";
    // A link issued in the last few minutes is reused instead of mailing another one.
    static final Duration ESPERA_REENVIO = Duration.ofMinutes(2);
    private static final Duration ESPERA_AVISO_CUENTA_EXISTENTE = Duration.ofMinutes(30);

    public static final String MENSAJE_REGISTRO =
            "Te enviamos un email para confirmar tu cuenta. Abrí el link para terminar el registro (revisá también la carpeta de spam).";

    // Last "you already have an account" notice per email, to avoid mail-bombing an owner.
    private final Map<String, Instant> ultimoAvisoCuentaExistente = new ConcurrentHashMap<>();

    // Compared against when the email does not exist, to keep login timing uniform.
    private String dummyPasswordHash;

    @PostConstruct
    void initDummyHash() {
        dummyPasswordHash = encoder.encode(UUID.randomUUID().toString());
    }

    // ============================== Registration ==============================

    /**
     * Never logs in and never says whether the email was taken:
     * - new email: creates the account unconfirmed and mails a confirmation link;
     * - unconfirmed account: resends the link without touching its data;
     * - GUEST (has guest orders): mails the claim link;
     * - confirmed account: mails the owner a notice instead.
     */
    public void register(UsuarioRequest dto) {
        if (!Boolean.TRUE.equals(dto.getTerminosAceptados())) {
            throw new BussinesException("Debes aceptar los términos y condiciones");
        }

        Usuario existente = usuarioRepo.findByEmail(dto.getEmail()).orElse(null);
        if (existente != null) {
            if (existente.getRol() == ROL.GUEST) {
                iniciarReclamoGuest(existente);
            } else if (!existente.isEmailConfirmado()) {
                // Data is NOT overwritten: otherwise anyone could swap the pending
                // password of someone else's account. The link is just resent.
                enviarLinkConfirmacion(existente, true);
            } else {
                avisarCuentaExistente(existente);
            }
            return;
        }

        Usuario u = usuarioMapper.toEntity(dto);
        aplicarDatosRegistro(u, dto);
        u.setRol(ROL.USER);
        u.setEmailVerificado(false);
        u.setSesionVersion(0);
        Usuario guardado = usuarioRepo.save(u);
        auditService.registrar("INSERT", "usuario", guardado.getUuid(), "Registro de cuenta (falta confirmar el email)");
        crearCarritoSiFalta(guardado);
        enviarLinkConfirmacion(guardado, true);
    }

    private void aplicarDatosRegistro(Usuario u, UsuarioRequest dto) {
        u.setNombre(dto.getNombre());
        u.setPassword(encoder.encode(dto.getPassword()));
        u.setTerminosAceptados(true);
        u.setVersionTerminosYCondicionesAceptados(dto.getVersionTerminosYCondicionesAceptados());
        u.setIpUsuario(dto.getIpUsuario());
        if (dto.getFechaAceptacionTerminos() != null) {
            u.setFechaAceptacionTerminos(FechaUtils.parseFechaTerminos(dto.getFechaAceptacionTerminos()));
        }
    }

    /**
     * Opens the confirmation link. The password chosen at registration is asked
     * again: if someone else registered this email first, the mailbox owner does
     * not know that password and cannot confirm the stranger's account (they use
     * "forgot password" instead, which proves the mailbox and sets their own).
     */
    public AuthResponse confirmarEmail(String token, String password) {
        Usuario user = usuarioRepo.findByTokenVerificacionHash(sha256(token))
                .filter(u -> u.getRol() != ROL.GUEST)
                .filter(u -> u.getTokenVerificacionExpira() != null
                        && u.getTokenVerificacionExpira().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new BussinesException("El link de confirmación es inválido o expiró. Registrate de nuevo para recibir otro."));

        long minutos = loginAttemptService.minutosBloqueado(user.getEmail());
        if (minutos > 0) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos fallidos. Probá de nuevo en " + minutos + " minutos o restablecé tu contraseña.");
        }
        if (user.getPassword() == null || !encoder.matches(password, user.getPassword())) {
            loginAttemptService.registrarFallo(user.getEmail());
            throw new BussinesException("La contraseña no coincide con la que elegiste al registrarte. Si no te registraste vos, usá \"Olvidé mi contraseña\" para elegir una.");
        }
        loginAttemptService.registrarExito(user.getEmail());

        user.setEmailVerificado(true);
        user.setTokenVerificacionHash(null);
        user.setTokenVerificacionExpira(null);
        Usuario guardado = usuarioRepo.save(user);
        auditService.registrar("UPDATE", "usuario", guardado.getUuid(), "Confirmó su email: cuenta activa");
        return nuevaSesion(guardado);
    }

    /**
     * Mails a confirmation link, reusing the throttle window: if a link went out in
     * the last minutes nothing is sent. {@code obligatorio} reports mail failures
     * (registration); login resends silently.
     */
    private void enviarLinkConfirmacion(Usuario u, boolean obligatorio) {
        String token = emitirTokenVerificacion(u);
        if (token == null) return;
        try {
            emailService.enviarConfirmacionRegistro(u, token);
        } catch (Exception e) {
            log.error("No se pudo enviar la confirmación de registro: {}", e.getMessage());
            if (obligatorio) {
                throw new BussinesException("No pudimos enviarte el email de confirmación. Intentá de nuevo más tarde");
            }
        }
    }

    /** New verification token (stored hashed), or null if one was issued moments ago. */
    private String emitirTokenVerificacion(Usuario u) {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime expira = u.getTokenVerificacionExpira();
        if (expira != null && expira.minus(VALIDEZ_TOKEN_VERIFICACION).plus(ESPERA_REENVIO).isAfter(ahora)) {
            return null;
        }
        String token = tokenAleatorio();
        u.setTokenVerificacionHash(sha256(token));
        u.setTokenVerificacionExpira(ahora.plus(VALIDEZ_TOKEN_VERIFICACION));
        usuarioRepo.save(u);
        return token;
    }

    private void avisarCuentaExistente(Usuario u) {
        String clave = u.getEmail().toLowerCase(Locale.ROOT);
        Instant ahora = Instant.now();
        Instant anterior = ultimoAvisoCuentaExistente.get(clave);
        if (anterior != null && anterior.plus(ESPERA_AVISO_CUENTA_EXISTENTE).isAfter(ahora)) return;
        ultimoAvisoCuentaExistente.put(clave, ahora);
        try {
            emailService.enviarAvisoCuentaExistente(u);
        } catch (Exception e) {
            log.error("No se pudo enviar el aviso de cuenta existente: {}", e.getMessage());
        }
    }

    // ============================== GUEST claim ==============================

    private void iniciarReclamoGuest(Usuario guest) {
        String token = emitirTokenVerificacion(guest);
        if (token == null) return;
        try {
            emailService.enviarVerificacionCuenta(guest, token);
        } catch (Exception e) {
            throw new BussinesException("No pudimos enviarte el email de verificación. Intentá de nuevo más tarde");
        }
    }

    public AuthResponse confirmarReclamoGuest(VerificarCuentaRequest dto) {
        if (!Boolean.TRUE.equals(dto.getTerminosAceptados())) {
            throw new BussinesException("Debes aceptar los términos y condiciones");
        }

        Usuario user = usuarioRepo.findByTokenVerificacionHash(sha256(dto.getToken()))
                .filter(u -> u.getRol() == ROL.GUEST)
                .filter(u -> u.getTokenVerificacionExpira() != null
                        && u.getTokenVerificacionExpira().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new BussinesException("El link de verificación es inválido o expiró"));

        user.setRol(ROL.USER);
        user.setPassword(encoder.encode(dto.getPassword()));
        user.setEmailVerificado(true);
        user.invalidarSesiones();
        user.setTerminosAceptados(true);
        user.setVersionTerminosYCondicionesAceptados(dto.getVersionTerminosYCondicionesAceptados());
        user.setIpUsuario(dto.getIpUsuario());
        if (dto.getFechaAceptacionTerminos() != null) {
            user.setFechaAceptacionTerminos(FechaUtils.parseFechaTerminos(dto.getFechaAceptacionTerminos()));
        }
        user.setTokenVerificacionHash(null);
        user.setTokenVerificacionExpira(null);

        Usuario usuarioGuardado = usuarioRepo.save(user);
        auditService.registrar("UPDATE", "usuario", usuarioGuardado.getUuid(), "Activó su cuenta de invitado: ahora es cliente");
        crearCarritoSiFalta(usuarioGuardado);
        return nuevaSesion(usuarioGuardado);
    }

    // ============================== Login / sessions ==============================

    public AuthResponse login(LoginRequest request) {
        long minutos = loginAttemptService.minutosBloqueado(request.getEmail());
        if (minutos > 0) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos fallidos. Probá de nuevo en " + minutos + " minuto" + (minutos == 1 ? "" : "s") + " o restablecé tu contraseña.");
        }

        Usuario u = usuarioRepo.findByEmail(request.getEmail()).orElse(null);

        // Same status, message and (roughly) timing whether the email exists or not.
        // GUEST accounts have no password and can never log in.
        String hash = (u != null && u.getPassword() != null) ? u.getPassword() : dummyPasswordHash;
        boolean passwordOk = encoder.matches(request.getPassword(), hash);
        if (u == null || u.getPassword() == null || u.getRol() == ROL.GUEST || !passwordOk) {
            loginAttemptService.registrarFallo(request.getEmail());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email o contraseña incorrectos");
        }
        loginAttemptService.registrarExito(request.getEmail());

        // Only reached with the right password, so it reveals nothing to a stranger.
        if (!u.isEmailConfirmado()) {
            enviarLinkConfirmacion(u, false);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Todavía no confirmaste tu email. Te enviamos el link de confirmación a tu casilla.");
        }

        return nuevaSesion(u);
    }

    /**
     * Renews a still-valid token. The new one keeps the original login time, so a
     * session can never be stretched past 7 days, and it is refused once the user
     * logged out, changed password or was demoted (session version changed).
     */
    public AuthResponse refreshToken(String currentToken) {
        Claims claims = jwtService.leerClaims(currentToken);
        Usuario usuario = claims == null ? null : usuarioRepo.findByEmail(claims.getSubject()).orElse(null);
        if (usuario == null || usuario.getRol() == ROL.GUEST || !jwtService.sesionVigente(claims, usuario)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La sesión venció. Ingresá de nuevo.");
        }
        String token = jwtService.generarToken(usuario, jwtService.authTime(claims));
        return new AuthResponse(token, usuarioMapper.toResponse(usuario));
    }

    /** Closes every session of the user (all devices): their tokens stop working at once. */
    public void logout(String email) {
        if (email == null) return;
        usuarioRepo.findByEmail(email).ifPresent(u -> {
            u.invalidarSesiones();
            usuarioRepo.save(u);
        });
    }

    // ============================== Password reset ==============================

    /** Always answers the same; only real, non-GUEST accounts get the email. */
    public void olvideContrasena(String email) {
        Usuario u = usuarioRepo.findByEmail(email).orElse(null);
        if (u == null || u.getRol() == ROL.GUEST) return;

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime expira = u.getTokenResetExpira();
        if (expira != null && expira.minus(VALIDEZ_TOKEN_RESET).plus(ESPERA_REENVIO).isAfter(ahora)) return;

        String token = tokenAleatorio();
        u.setTokenResetHash(sha256(token));
        u.setTokenResetExpira(ahora.plus(VALIDEZ_TOKEN_RESET));
        usuarioRepo.save(u);
        try {
            emailService.enviarRestablecerContrasena(u, token);
        } catch (Exception e) {
            log.error("No se pudo enviar el email de restablecimiento: {}", e.getMessage());
        }
    }

    /**
     * Sets the new password from a one-time link. Opening the link proves the
     * mailbox, so it also confirms the email; every other session is closed.
     */
    public AuthResponse restablecerContrasena(RestablecerContrasenaRequest dto) {
        Usuario u = usuarioRepo.findByTokenResetHash(sha256(dto.getToken()))
                .filter(x -> x.getRol() != ROL.GUEST)
                .filter(x -> x.getTokenResetExpira() != null && x.getTokenResetExpira().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new BussinesException("El link para restablecer la contraseña es inválido o expiró. Pedí uno nuevo."));

        u.setPassword(encoder.encode(dto.getPassword()));
        u.setTokenResetHash(null);
        u.setTokenResetExpira(null);
        u.setEmailVerificado(true);
        u.invalidarSesiones();
        Usuario guardado = usuarioRepo.save(u);
        auditService.registrar("UPDATE", "usuario", guardado.getUuid(), "Restableció la contraseña con el link del email (sesiones cerradas)");
        loginAttemptService.registrarExito(guardado.getEmail());
        return nuevaSesion(guardado);
    }

    /*
     * Changes the password of the logged-in user after checking the current one
     * (failures count toward the same lockout as the login). Every session is
     * closed and a fresh token is returned so this device stays signed in.
     */
    public AuthResponse cambiarContrasena(String email, CambiarContrasenaRequest dto) {
        Usuario u = email == null ? null : usuarioRepo.findByEmail(email)
                .filter(x -> x.getRol() != ROL.GUEST && x.getPassword() != null)
                .orElse(null);
        if (u == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La sesión venció. Ingresá de nuevo.");
        }

        long minutos = loginAttemptService.minutosBloqueado(u.getEmail());
        if (minutos > 0) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos fallidos. Probá de nuevo en " + minutos + " minuto" + (minutos == 1 ? "" : "s") + ".");
        }
        if (!encoder.matches(dto.getActual(), u.getPassword())) {
            loginAttemptService.registrarFallo(u.getEmail());
            throw new BussinesException("La contraseña actual no es correcta");
        }
        loginAttemptService.registrarExito(u.getEmail());
        if (encoder.matches(dto.getNueva(), u.getPassword())) {
            throw new BussinesException("La contraseña nueva tiene que ser distinta de la actual");
        }

        u.setPassword(encoder.encode(dto.getNueva()));
        u.setTokenResetHash(null);
        u.setTokenResetExpira(null);
        u.invalidarSesiones();
        Usuario guardado = usuarioRepo.save(u);
        auditService.registrar("UPDATE", "usuario", guardado.getUuid(), "Cambió su contraseña desde el perfil (otras sesiones cerradas)");
        return nuevaSesion(guardado);
    }

    // ============================== Email change ==============================

    /*
     * Starts an email change for the logged-in user: checks the password (failures count
     * toward the login lockout) and mails a link to the NEW address. When that address
     * already has an account nothing is sent, but the answer is the same, so nobody can
     * use this to find out which emails are registered.
     */
    @Transactional
    public void solicitarCambioEmail(String email, CambiarEmailRequest dto) {
        Usuario u = email == null ? null : usuarioRepo.findByEmail(email)
                .filter(x -> x.getRol() != ROL.GUEST && x.getPassword() != null)
                .orElse(null);
        if (u == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La sesión venció. Ingresá de nuevo.");
        }

        long minutos = loginAttemptService.minutosBloqueado(u.getEmail());
        if (minutos > 0) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos fallidos. Probá de nuevo en " + minutos + " minuto" + (minutos == 1 ? "" : "s") + ".");
        }
        if (!encoder.matches(dto.getPassword(), u.getPassword())) {
            loginAttemptService.registrarFallo(u.getEmail());
            throw new BussinesException("La contraseña no es correcta");
        }
        loginAttemptService.registrarExito(u.getEmail());

        String nuevo = dto.getNuevoEmail().trim();
        if (nuevo.equalsIgnoreCase(u.getEmail())) {
            throw new BussinesException("Ese ya es el email de tu cuenta");
        }
        if (usuarioRepo.findByEmail(nuevo).isPresent()) {
            auditService.registrar("UPDATE", "usuario", u.getUuid(), "Pidió cambiar su email (el email nuevo ya tenía cuenta: no se envió el link)");
            return;
        }

        String token = tokenAleatorio();
        u.setEmailPendiente(nuevo);
        u.setTokenEmailHash(sha256(token));
        u.setTokenEmailExpira(LocalDateTime.now().plus(VALIDEZ_TOKEN_EMAIL));
        usuarioRepo.save(u);
        try {
            emailService.enviarConfirmacionCambioEmail(u, nuevo, token);
        } catch (Exception e) {
            log.error("No se pudo enviar el email de cambio de dirección: {}", e.getMessage());
            throw new BussinesException("No pudimos enviar el email al nuevo email. Revisalo e intentá de nuevo más tarde");
        }
        auditService.registrar("UPDATE", "usuario", u.getUuid(), "Pidió cambiar su email (link enviado al email nuevo)");
    }

    /*
     * Applies the change from the link: opening it proves the new mailbox. Every session
     * is closed (the user signs in again with the new email) and the old address is told.
     */
    @Transactional
    public void confirmarCambioEmail(String token) {
        Usuario u = usuarioRepo.findByTokenEmailHash(sha256(token))
                .filter(x -> x.getEmailPendiente() != null)
                .filter(x -> x.getTokenEmailExpira() != null && x.getTokenEmailExpira().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new BussinesException("El link para cambiar el email es inválido o expiró. Pedí el cambio de nuevo desde tu perfil."));

        String nuevo = u.getEmailPendiente();
        if (usuarioRepo.findByEmail(nuevo).isPresent()) {
            limpiarCambioEmail(u);
            usuarioRepo.save(u);
            throw new BussinesException("Ese email ya está en uso en otra cuenta.");
        }

        String anterior = u.getEmail();
        u.setEmail(nuevo);
        u.setEmailVerificado(true);
        limpiarCambioEmail(u);
        u.invalidarSesiones();
        Usuario guardado = usuarioRepo.save(u);
        auditService.registrar("UPDATE", "usuario", guardado.getUuid(), "Confirmó el cambio de su email (sesiones cerradas)");
        try {
            emailService.enviarAvisoEmailCambiado(guardado, anterior);
        } catch (Exception e) {
            log.error("No se pudo avisar el cambio de email a la dirección anterior: {}", e.getMessage());
        }
    }

    private static void limpiarCambioEmail(Usuario u) {
        u.setEmailPendiente(null);
        u.setTokenEmailHash(null);
        u.setTokenEmailExpira(null);
    }

    // ============================== helpers ==============================

    private AuthResponse nuevaSesion(Usuario u) {
        return new AuthResponse(jwtService.generarToken(u), usuarioMapper.toResponse(u));
    }

    private void crearCarritoSiFalta(Usuario u) {
        if (carritoRepo.findByUsuario(u).isEmpty()) {
            Carrito carrito = new Carrito();
            carrito.setUsuario(u);
            carrito.setFechaCreacion(LocalDateTime.now());
            carritoRepo.save(carrito);
        }
    }

    private static String tokenAleatorio() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
