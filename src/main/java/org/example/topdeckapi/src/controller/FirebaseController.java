package org.example.topdeckapi.src.controller;

import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.model.Usuario;
import org.example.topdeckapi.src.service.IMPL.FirebaseTokenService;
import org.example.topdeckapi.src.service.IMPL.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/firebase")
@RequiredArgsConstructor
public class FirebaseController {

    private final FirebaseTokenService firebaseTokenService;
    private final UsuarioService usuarioService;

    // Protected by SecurityConfig ("/firebase/admin/**" -> ROLE_ADMIN).
    @GetMapping("/admin/token")
    public ResponseEntity<Map<String, String>> obtenerToken() {
        Usuario admin = usuarioService.obtenerUsuarioAutenticado();
        if (admin == null) {
            throw new AccessDeniedException("Usuario no autenticado");
        }
        return ResponseEntity.ok(Map.of("token", firebaseTokenService.crearTokenAdmin(admin.getUuid())));
    }
}
