package org.example.topdeckapi.src.service.IMPL;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.example.topdeckapi.src.DTOs.mappers.UsuarioMapper;
import org.example.topdeckapi.src.DTOs.request.UsuarioRequest;
import org.example.topdeckapi.src.DTOs.response.PaginacionResponse;
import org.example.topdeckapi.src.DTOs.response.UsuarioResponse;
import org.example.topdeckapi.src.Enumerados.ROL;
import org.example.topdeckapi.src.Repository.IUsuarioRepo;
import org.example.topdeckapi.src.model.Usuario;
import org.example.topdeckapi.src.service.Interface.IUsuarioService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import java.util.Map;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UsuarioService implements IUsuarioService {
    private final IUsuarioRepo usuarioRepo;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;
    private final PaginacionService paginacionService;

    //METODOS PARA OBTENER DATOS PAGINADOS Y DATOS PRECISOS

    private Sort buildSort (String sortBy, String direction){
        Map<String,String> mapeoCampos = Map.of(
                "idUsuario", "idUsuario",
                "nombre", "nombre",
                "email","email"
        );

        String campoReal = mapeoCampos.getOrDefault(sortBy,"idUsuario");
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, campoReal);
    }


    public PaginacionResponse<UsuarioResponse> obtenerPaginados (Integer pagina, Integer tamanio, String sortBy, String direction) {
        Sort sort = buildSort (sortBy, direction);
        Pageable pageable = PageRequest.of(pagina - 1, tamanio, sort);
        Page<Usuario> paginaUsuarios = usuarioRepo.findAll(pageable);
        return paginacionService.crearPaginacionResponse(paginaUsuarios,tamanio,pagina, usuarioMapper::toResponse);
    }

    public PaginacionResponse<UsuarioResponse> obtenerPaginadosConFiltro(Integer pagina, Integer tamanio, String sortBy, String direction, String filtro) {
        Sort sort = buildSort (sortBy, direction);
        Pageable pageable = PageRequest.of(pagina - 1, tamanio, sort);
        Page<Usuario> paginaUsuarios = usuarioRepo.findBySearch(filtro, pageable);
        return paginacionService.crearPaginacionResponse(paginaUsuarios,tamanio,pagina, usuarioMapper::toResponse);
    }

    public UsuarioResponse obtenerPorId(Long id){
        if(usuarioRepo.existsById(id)){
            return usuarioMapper.toResponse(usuarioRepo.findById(id).orElseThrow(
                    () -> new RuntimeException("Usuario no encontrado")
            ));
        }else{
            throw new RuntimeException("Usuario no encontrado");
        }
    }

    public Usuario obtenerUsuarioAutenticado(){
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
                System.out.println("No hay usuario autenticado o es anónimo");
                return null;
            }

            String email = auth.getName();
            System.out.println("Email del usuario autenticado: " + email);

            Usuario usuario = usuarioRepo.findByEmail(email).orElse(null);

            if (usuario != null) {
                System.out.println("Usuario encontrado: " + usuario.getEmail() + ", Rol: " + usuario.getRol());
            } else {
                System.out.println("Usuario no encontrado en BD con email: " + email);
            }

            return usuario;
        } catch (Exception e) {
            System.err.println("Error al obtener usuario autenticado: " + e.getMessage());
            return null;
        }
    }

    //METODOS PARA CREAR DATOS
    public UsuarioResponse guardar (UsuarioRequest newUsuario){
        if(usuarioRepo.existsByEmail(newUsuario.getEmail())){
            throw new RuntimeException("El email ya existe en el sistema");
        }

        Usuario usuario = usuarioMapper.toEntity(newUsuario);
        usuario.setPassword(passwordEncoder.encode(newUsuario.getPassword()));
        Usuario usuarioGuardado = usuarioRepo.save(usuario);
        return usuarioMapper.toResponse(usuario);
    }

    public UsuarioResponse actualizarUsuario(UsuarioRequest request, Long id){
        Usuario usuario = usuarioRepo.findById(id).orElseThrow(
                ()-> new RuntimeException("Usuario no encontrado")
        );

        if(request.getEmail() != null
            && usuarioRepo.existsByEmail(request.getEmail())){
            throw new RuntimeException("El email ya existe en el sistema");
        }

        Usuario usuarioActualizado = usuarioMapper.toEntity(request);
        usuarioActualizado.setIdUsuario(id);
        usuarioActualizado.setNombre(request.getNombre());
        usuarioActualizado.setEmail(request.getEmail());
        usuarioActualizado.setTerminosAceptados(request.getTerminosAceptados());
        usuarioActualizado.setVersionTerminosYCondicionesAceptados(request.getVersionTerminosYCondicionesAceptados());
        if(request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            usuarioActualizado.setPassword(passwordEncoder.encode(request.getPassword()));
        }else{
            usuarioActualizado.setPassword(usuario.getPassword());
        }

        Usuario usuarioGuardado = usuarioRepo.save(usuarioActualizado);
        return usuarioMapper.toResponse(usuarioGuardado);
    }

    public Usuario crearUsuarioEfimero(UsuarioRequest newUsuario){
        Optional<Usuario> usuarioExistente = usuarioRepo.findByEmail(newUsuario.getEmail());

        if (usuarioExistente.isPresent()) {
            Usuario usuario = usuarioExistente.get();
            if (usuario.getRol() == ROL.GUESS) {
                return usuario;
            }
            throw new RuntimeException("El email ya está registrado con un usuario de tipo: " + usuario.getRol());
        }
        Usuario usuario = usuarioMapper.toEntity(newUsuario);
        usuario.setRol(ROL.GUESS);
        return usuarioRepo.save(usuario);
    }

    public boolean deleteUsuario(Long id){
        if(usuarioRepo.existsById(id)){
            usuarioRepo.deleteById(id);
            return true;
        }else{
            return false;
        }
    }
}
