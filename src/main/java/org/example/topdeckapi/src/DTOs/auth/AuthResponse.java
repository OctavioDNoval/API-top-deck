package org.example.topdeckapi.src.DTOs.auth;

import lombok.Getter;
import lombok.Setter;
import org.example.topdeckapi.src.DTOs.DTO.UsuarioDTO;

@Getter
@Setter
public class AuthResponse {
    private String token;
    private UsuarioDTO usuario;

    public AuthResponse(String token, UsuarioDTO usuario) {
        this.token = token;
        this.usuario = usuario;
    }
}
