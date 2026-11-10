package org.example.topdeckapi.src.service.IMPL;

import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.auth.AuthResponse;
import org.example.topdeckapi.src.DTOs.auth.LoginRequest;
import org.example.topdeckapi.src.DTOs.mappers.UsuarioMapper;
import org.example.topdeckapi.src.DTOs.request.UsuarioRequest;
import org.example.topdeckapi.src.DTOs.request.VerificarCuentaRequest;
import org.example.topdeckapi.src.DTOs.response.UsuarioResponse;
import org.example.topdeckapi.src.Enumerados.ROL;
import org.example.topdeckapi.src.Exception.BussinesException;
import org.example.topdeckapi.src.Exception.UsuarioNotFoundException;
import org.example.topdeckapi.src.Repository.ICarritoRepository;
import org.example.topdeckapi.src.Repository.IUsuarioRepo;
import org.example.topdeckapi.src.model.Carrito;
import org.example.topdeckapi.src.model.Usuario;
import org.example.topdeckapi.src.util.FechaUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final IUsuarioRepo usuarioRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final ICarritoRepository  carritoRepo;
    private final UsuarioMapper usuarioMapper;
    private final AuditService auditService;
    private final EmailService emailService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Duration VALIDEZ_TOKEN_RECLAMO = Duration.ofHours(24);
    private static final Duration ESPERA_REENVIO = Duration.ofMinutes(2);


    /*
    * Returns empty when the email belongs to a GUEST: the account is not
    * upgraded here (anyone could type that email). Instead a one-time link is
    * mailed to the owner, who sets the password in confirmarReclamoGuest.
    * */
    public Optional<AuthResponse> register (UsuarioRequest dto){
        if (!Boolean.TRUE.equals(dto.getTerminosAceptados())) {
            throw new BussinesException("Debes aceptar los términos y condiciones");
        }

        Optional<Usuario> existingUser = usuarioRepo.findByEmail(dto.getEmail());

        if (existingUser.isPresent()) {
            Usuario user = existingUser.get();
            if (user.getRol() != ROL.GUEST) {
                throw new BussinesException("El usuario con ese mail ya existe");
            }
            iniciarReclamoGuest(user);
            return Optional.empty();
        }

        Usuario u = usuarioMapper.toEntity(dto);
        u.setPassword(encoder.encode(dto.getPassword()));
        u.setRol(ROL.USER);
        if (dto.getFechaAceptacionTerminos() != null) {
            u.setFechaAceptacionTerminos(
                FechaUtils.parseFechaTerminos(dto.getFechaAceptacionTerminos())
            );
        }

        Usuario usuarioGuardado = usuarioRepo.save(u);

        auditService.registrar("INSERT", "usuario");

        Carrito nuevoCarrito = new Carrito();
        nuevoCarrito.setUsuario(usuarioGuardado);
        nuevoCarrito.setFechaCreacion(LocalDateTime.now());
        carritoRepo.save(nuevoCarrito);

        UserDetails userDetails = User.withUsername(usuarioGuardado.getEmail())
                .password("")
                .roles(usuarioGuardado.getRol().name())
                .build();

        String token = jwtService.generateToken(userDetails);

        UsuarioResponse uResponse = usuarioMapper.toResponse(usuarioGuardado);
        return Optional.of(new AuthResponse(token,uResponse));
    }

    private void iniciarReclamoGuest(Usuario guest) {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime expira = guest.getTokenVerificacionExpira();
        // Avoid mail-bombing: a link issued in the last few minutes is still pending.
        if (expira != null && expira.minus(VALIDEZ_TOKEN_RECLAMO).plus(ESPERA_REENVIO).isAfter(ahora)) {
            return;
        }

        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        guest.setTokenVerificacionHash(sha256(token));
        guest.setTokenVerificacionExpira(ahora.plus(VALIDEZ_TOKEN_RECLAMO));
        usuarioRepo.save(guest);

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
        user.setTerminosAceptados(true);
        user.setVersionTerminosYCondicionesAceptados(dto.getVersionTerminosYCondicionesAceptados());
        user.setIpUsuario(dto.getIpUsuario());
        if (dto.getFechaAceptacionTerminos() != null) {
            user.setFechaAceptacionTerminos(FechaUtils.parseFechaTerminos(dto.getFechaAceptacionTerminos()));
        }
        user.setTokenVerificacionHash(null);
        user.setTokenVerificacionExpira(null);

        Usuario usuarioGuardado = usuarioRepo.save(user);
        auditService.registrar("UPDATE", "usuario");

        if (carritoRepo.findByUsuario(usuarioGuardado).isEmpty()) {
            Carrito nuevoCarrito = new Carrito();
            nuevoCarrito.setUsuario(usuarioGuardado);
            nuevoCarrito.setFechaCreacion(LocalDateTime.now());
            carritoRepo.save(nuevoCarrito);
        }

        String token = jwtService.generateToken(User.withUsername(usuarioGuardado.getEmail())
                .password("")
                .roles(usuarioGuardado.getRol().name())
                .build());
        return new AuthResponse(token, usuarioMapper.toResponse(usuarioGuardado));
    }

    private static String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public AuthResponse login (LoginRequest request){
        Usuario u = usuarioRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsuarioNotFoundException("Usuario con ese mail no encontrado"));

        if(!encoder.matches(request.getPassword(), u.getPassword())){
            throw new BussinesException("Contraseña o Email incorrectos");
        }

        String token = jwtService.generateToken(
                User.withUsername(u.getEmail())
                        .password(u.getPassword())
                        .roles(u.getRol().name())
                        .build()
        );

        UsuarioResponse uDTO = usuarioMapper.toResponse(u);

        return new AuthResponse(token,uDTO);
    }

    public AuthResponse refreshToken(String currentToken) {
        if (!jwtService.isTokenValid(currentToken)) {
            throw new BussinesException("Token inválido o expirado");
        }

        String email = jwtService.extractEmail(currentToken);
        Usuario usuario = usuarioRepo.findByEmail(email)
                .orElseThrow(() -> new UsuarioNotFoundException("Usuario no encontrado"));

        String newToken = jwtService.generateRefreshToken(currentToken);
        UsuarioResponse uResponse = usuarioMapper.toResponse(usuario);
        return new AuthResponse(newToken, uResponse);
    }
}
