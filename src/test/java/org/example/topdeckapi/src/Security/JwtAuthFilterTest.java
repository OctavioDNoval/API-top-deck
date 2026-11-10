package org.example.topdeckapi.src.Security;

import org.example.topdeckapi.src.Enumerados.ROL;
import org.example.topdeckapi.src.Repository.IUsuarioRepo;
import org.example.topdeckapi.src.model.Usuario;
import org.example.topdeckapi.src.service.IMPL.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthFilterTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-0123456789";

    private JwtService jwtService;
    private IUsuarioRepo usuarioRepo;
    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET);
        usuarioRepo = mock(IUsuarioRepo.class);
        filter = new JwtAuthFilter(jwtService, usuarioRepo);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static Usuario usuario(String email, ROL rol, int sesionVersion) {
        Usuario u = new Usuario();
        u.setEmail(email);
        u.setRol(rol);
        u.setSesionVersion(sesionVersion);
        return u;
    }

    private Authentication filterWithToken(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/user/admin/obtenerPaginados");
        request.addHeader("Authorization", "Bearer " + token);
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Test
    void roleComesFromTheDatabaseNotFromTheToken() throws Exception {
        // Validly signed token issued while the user was ADMIN; the DB now says USER.
        String token = jwtService.generarToken(usuario("user@example.com", ROL.ADMIN, 0));
        when(usuarioRepo.findByEmail("user@example.com")).thenReturn(Optional.of(usuario("user@example.com", ROL.USER, 0)));

        Authentication auth = filterWithToken(token);

        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_USER");
    }

    @Test
    void tokenOfDeletedUserIsTreatedAsAnonymous() throws Exception {
        String token = jwtService.generarToken(usuario("borrado@example.com", ROL.USER, 0));
        when(usuarioRepo.findByEmail("borrado@example.com")).thenReturn(Optional.empty());

        assertThat(filterWithToken(token)).isNull();
    }

    @Test
    void tokenSignedWithAnotherSecretIsIgnored() throws Exception {
        String foreign = new JwtService("otro-secret-otro-secret-otro-secret-0123456789")
                .generarToken(usuario("user@example.com", ROL.ADMIN, 0));
        when(usuarioRepo.findByEmail("user@example.com")).thenReturn(Optional.of(usuario("user@example.com", ROL.ADMIN, 0)));

        assertThat(filterWithToken(foreign)).isNull();
    }

    @Test
    void tokenIssuedBeforeLogoutOrPasswordChangeIsRejected() throws Exception {
        String token = jwtService.generarToken(usuario("user@example.com", ROL.USER, 0));
        // Logout / new password bumped the version to 1.
        when(usuarioRepo.findByEmail("user@example.com")).thenReturn(Optional.of(usuario("user@example.com", ROL.USER, 1)));

        assertThat(filterWithToken(token)).isNull();
    }

    @Test
    void sessionOlderThanSevenDaysIsRejected() throws Exception {
        Usuario u = usuario("user@example.com", ROL.USER, 0);
        String token = jwtService.generarToken(u, Instant.now().minus(Duration.ofDays(8)));
        when(usuarioRepo.findByEmail("user@example.com")).thenReturn(Optional.of(u));

        assertThat(filterWithToken(token)).isNull();
    }

    @Test
    void unconfirmedEmailGetsNoSession() throws Exception {
        Usuario u = usuario("nuevo@example.com", ROL.USER, 0);
        u.setEmailVerificado(false);
        String token = jwtService.generarToken(u);
        when(usuarioRepo.findByEmail("nuevo@example.com")).thenReturn(Optional.of(u));

        assertThat(filterWithToken(token)).isNull();
    }
}
