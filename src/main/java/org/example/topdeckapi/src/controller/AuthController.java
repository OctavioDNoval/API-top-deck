package org.example.topdeckapi.src.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.auth.AuthResponse;
import org.example.topdeckapi.src.DTOs.auth.LoginRequest;
import org.example.topdeckapi.src.DTOs.request.ConfirmarEmailRequest;
import org.example.topdeckapi.src.DTOs.request.OlvideContrasenaRequest;
import org.example.topdeckapi.src.DTOs.request.RestablecerContrasenaRequest;
import org.example.topdeckapi.src.DTOs.request.UsuarioRequest;
import org.example.topdeckapi.src.DTOs.request.VerificarCuentaRequest;
import org.example.topdeckapi.src.Security.ClientIpResolver;
import org.example.topdeckapi.src.service.IMPL.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final ClientIpResolver clientIpResolver;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    /** Always 202 with the same message: it never tells whether the email already had an account. */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody UsuarioRequest dto, HttpServletRequest request) {
        dto.setIpUsuario(clientIpResolver.resolve(request));
        authService.register(dto);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "verificacionPendiente", true,
                "message", AuthService.MENSAJE_REGISTRO));
    }

    @PostMapping("/confirmar-email")
    public ResponseEntity<AuthResponse> confirmarEmail(@Valid @RequestBody ConfirmarEmailRequest dto) {
        return ResponseEntity.ok(authService.confirmarEmail(dto.getToken(), dto.getPassword()));
    }

    @PostMapping("/verificar-cuenta")
    public ResponseEntity<AuthResponse> verificarCuenta(@Valid @RequestBody VerificarCuentaRequest dto, HttpServletRequest request) {
        dto.setIpUsuario(clientIpResolver.resolve(request));
        return ResponseEntity.ok(authService.confirmarReclamoGuest(dto));
    }

    /** Always 202 with the same message, whether or not the email has an account. */
    @PostMapping("/olvide-contrasena")
    public ResponseEntity<Map<String, Object>> olvideContrasena(@Valid @RequestBody OlvideContrasenaRequest dto) {
        authService.olvideContrasena(dto.getEmail());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "message", "Si el email tiene una cuenta, te enviamos un link para restablecer la contraseña. Vence en 1 hora."));
    }

    @PostMapping("/restablecer-contrasena")
    public ResponseEntity<AuthResponse> restablecerContrasena(@Valid @RequestBody RestablecerContrasenaRequest dto) {
        return ResponseEntity.ok(authService.restablecerContrasena(dto));
    }

    /** Revokes every session of the caller (all devices). Anonymous calls are a no-op. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {
        authService.logout(authentication != null ? authentication.getName() : null);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestHeader("Authorization") String token) {
        String cleanToken = token.replace("Bearer ", "").trim();
        return ResponseEntity.ok(authService.refreshToken(cleanToken));
    }
}
