package org.example.topdeckapi.src.controller;

import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.CreateDTO.CreateUsuarioDTO;
import org.example.topdeckapi.src.DTOs.CreateDTO.CreateUsuarioSinContraseniaDTO;
import org.example.topdeckapi.src.DTOs.auth.AuthResponse;
import org.example.topdeckapi.src.DTOs.auth.LoginRequest;
import org.example.topdeckapi.src.model.Usuario;
import org.example.topdeckapi.src.service.IMPL.AuthService;
import org.example.topdeckapi.src.service.IMPL.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register (@RequestBody CreateUsuarioDTO dto){
        return ResponseEntity.ok(authService.register(dto));
    }



    @GetMapping("/validate/start")
    public ResponseEntity<?> validate(@RequestHeader("Authorization") String token){
        try {
            String cleanToken = token.replace("Bearer ", "").trim();

            if (jwtService.isTokenValid(cleanToken)) {
                return ResponseEntity.ok(cleanToken); // Devuelve el token limpio
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Token inválido: " + e.getMessage());
        }
    }
}
