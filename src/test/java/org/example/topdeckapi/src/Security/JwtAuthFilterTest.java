package org.example.topdeckapi.src.Security;

import org.example.topdeckapi.src.service.IMPL.CustomUserDetailsService;
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
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthFilterTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-0123456789";

    private JwtService jwtService;
    private CustomUserDetailsService userDetailsService;
    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET);
        userDetailsService = mock(CustomUserDetailsService.class);
        filter = new JwtAuthFilter(jwtService, userDetailsService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Authentication filterWithToken(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/user/admin/obtenerPaginados");
        request.addHeader("Authorization", "Bearer " + token);
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Test
    void roleComesFromTheDatabaseNotFromTheToken() throws Exception {
        // Token validly signed but claiming ADMIN for someone who is USER in the DB.
        String forged = jwtService.generateToken(
                User.withUsername("user@example.com").password("").roles("ADMIN").build());
        when(userDetailsService.loadUserByUsername("user@example.com"))
                .thenReturn(User.withUsername("user@example.com").password("x").roles("USER").build());

        Authentication auth = filterWithToken(forged);

        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_USER");
    }

    @Test
    void tokenOfDeletedUserIsTreatedAsAnonymous() throws Exception {
        String token = jwtService.generateToken(
                User.withUsername("borrado@example.com").password("").roles("USER").build());
        when(userDetailsService.loadUserByUsername("borrado@example.com"))
                .thenThrow(new UsernameNotFoundException("no existe"));

        assertThat(filterWithToken(token)).isNull();
    }

    @Test
    void tokenSignedWithAnotherSecretIsIgnored() throws Exception {
        String foreign = new JwtService("otro-secret-otro-secret-otro-secret-0123456789")
                .generateToken(User.withUsername("user@example.com").password("").roles("ADMIN").build());

        assertThat(filterWithToken(foreign)).isNull();
    }
}
