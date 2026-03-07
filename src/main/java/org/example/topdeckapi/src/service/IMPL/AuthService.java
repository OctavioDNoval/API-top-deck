package org.example.topdeckapi.src.service.IMPL;

import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.auth.AuthResponse;
import org.example.topdeckapi.src.DTOs.auth.LoginRequest;
import org.example.topdeckapi.src.DTOs.mappers.UsuarioMapper;
import org.example.topdeckapi.src.DTOs.request.UsuarioRequest;
import org.example.topdeckapi.src.DTOs.response.UsuarioResponse;
import org.example.topdeckapi.src.Enumerados.ROL;
import org.example.topdeckapi.src.Exception.UsuarioNotFoundException;
import org.example.topdeckapi.src.Repository.ICarritoRepository;
import org.example.topdeckapi.src.Repository.IUsuarioRepo;
import org.example.topdeckapi.src.model.Carrito;
import org.example.topdeckapi.src.model.Usuario;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final IUsuarioRepo usuarioRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final ICarritoRepository  carritoRepo;
    private final UsuarioMapper usuarioMapper;


    public AuthResponse register (UsuarioRequest dto){
        if(usuarioRepo.existsByEmail(dto.getEmail())){
            throw new UsuarioNotFoundException("El usuario ya existe");
        }
        Usuario u = usuarioMapper.toEntity(dto);
        u.setPassword(encoder.encode(dto.getPassword()));
        if (u.getRol() == null){
            u.setRol(ROL.USER);
        }
        Usuario usuarioGuardado = usuarioRepo.save(u);

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
        return new AuthResponse(token,uResponse);
    }

    public AuthResponse login (LoginRequest request){
        Usuario u = usuarioRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsuarioNotFoundException("El usuario no existe"));

        if(!encoder.matches(request.getPassword(), u.getPassword())){
            throw new RuntimeException("Contraseña incorrecta");
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
}
