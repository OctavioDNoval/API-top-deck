package org.example.topdeckapi.src.Security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.topdeckapi.src.Enumerados.ROL;
import org.example.topdeckapi.src.Repository.IUsuarioRepo;
import org.example.topdeckapi.src.model.Usuario;
import org.example.topdeckapi.src.service.IMPL.JwtService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final IUsuarioRepo usuarioRepo;

    private static final String PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith(PREFIX)
                || SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Signature and expiry; a forged or expired token just stays anonymous.
        Claims claims = jwtService.leerClaims(authHeader.substring(PREFIX.length()));
        if (claims == null || claims.getSubject() == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Usuario usuario = usuarioRepo.findByEmail(claims.getSubject()).orElse(null);
        // GUESTs never get a session; a logged-out, re-passworded, unverified or
        // too-old session is rejected here even if the token itself is still valid.
        if (usuario == null || usuario.getRol() == ROL.GUEST || !jwtService.sesionVigente(claims, usuario)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Authorities come from the database, never from the token claim.
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()));
        User principal = new User(usuario.getEmail(), "", authorities);
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
        log.debug("Autenticado {} con {}", usuario.getEmail(), authorities);

        filterChain.doFilter(request, response);
    }
}
