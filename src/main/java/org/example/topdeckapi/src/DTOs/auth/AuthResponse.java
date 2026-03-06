package org.example.topdeckapi.src.DTOs.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.example.topdeckapi.src.DTOs.DTO.UsuarioDTO;
import org.example.topdeckapi.src.DTOs.response.UsuarioResponse;

@Getter
@Setter
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private UsuarioResponse usuario;
}
