package org.example.topdeckapi.integration;

import org.example.topdeckapi.src.Enumerados.ROL;
import org.example.topdeckapi.src.model.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** URL rules of SecurityConfig: /public open, /user logged in, /admin only ADMIN (role read from the DB). */
class SecurityRulesIntegrationTest extends AbstractIntegrationTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "/user/admin/obtenerPaginados",
            "/pedidos/admin/obtenerPaginados",
            "/stats/admin"
    })
    void adminEndpoints_withoutToken_are401(String url) throws Exception {
        mvc.perform(como(get(url), null)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/user/admin/obtenerPaginados",
            "/pedidos/admin/obtenerPaginados",
            "/stats/admin"
    })
    void adminEndpoints_asUser_are403(String url) throws Exception {
        String token = tokenDe(crearUsuario(ROL.USER));
        mvc.perform(como(get(url), token)).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/user/admin/obtenerPaginados",
            "/pedidos/admin/obtenerPaginados",
            "/stats/admin"
    })
    void adminEndpoints_asAdmin_are200(String url) throws Exception {
        String token = tokenDe(crearUsuario(ROL.ADMIN));
        mvc.perform(como(get(url), token)).andExpect(status().isOk());
    }

    @Test
    void demotedAdmin_losesAccessWithTheSameToken() throws Exception {
        // The role comes from the database on every request, never from the token.
        Usuario admin = crearUsuario(ROL.ADMIN);
        String token = tokenDe(admin);
        mvc.perform(como(get("/stats/admin"), token)).andExpect(status().isOk());

        admin.setRol(ROL.USER);
        usuarioRepo.save(admin);

        mvc.perform(como(get("/stats/admin"), token)).andExpect(status().isForbidden());
    }

    @Test
    void userEndpoint_withoutToken_is401() throws Exception {
        mvc.perform(como(get("/user/me"), null)).andExpect(status().isUnauthorized());
    }

    @Test
    void userEndpoint_withForgedToken_is401() throws Exception {
        mvc.perform(como(get("/user/me"), "not-a-real-token")).andExpect(status().isUnauthorized());
    }

    @Test
    void publicEndpoint_withoutToken_is200() throws Exception {
        mvc.perform(como(get("/products/public/ofertas"), null)).andExpect(status().isOk());
    }
}
