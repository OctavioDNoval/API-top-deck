package org.example.topdeckapi.src.controller;

import jakarta.validation.Valid;
import org.example.topdeckapi.src.DTOs.request.VerificarCuentaRequest;
import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.auth.AuthResponse;
import org.example.topdeckapi.src.DTOs.auth.LoginRequest;
import org.example.topdeckapi.src.DTOs.request.UsuarioRequest;
import org.example.topdeckapi.src.service.IMPL.AuthService;
import org.example.topdeckapi.src.service.IMPL.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register (@Valid @RequestBody UsuarioRequest dto){
        return authService.register(dto)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                        "verificacionPendiente", true,
                        "message", "Ya hiciste pedidos con este email. Te enviamos un link para activar tu cuenta."
                )));
    }

    @PostMapping("/verificar-cuenta")
    public ResponseEntity<AuthResponse> verificarCuenta(@Valid @RequestBody VerificarCuentaRequest dto){
        return ResponseEntity.ok(authService.confirmarReclamoGuest(dto));
    }

    @GetMapping("/validate/start")
    public ResponseEntity<?> validate(@RequestHeader("Authorization") String token){
        try {
            String cleanToken = token.replace("Bearer ", "").trim();

            if (jwtService.isTokenValid(cleanToken)) {
                return ResponseEntity.ok(cleanToken);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Token inválido: " + e.getMessage());
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestHeader("Authorization") String token){
        String cleanToken = token.replace("Bearer ", "").trim();
        return ResponseEntity.ok(authService.refreshToken(cleanToken));
    }
}
