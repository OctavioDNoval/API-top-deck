package org.example.topdeckapi.integration;

import org.example.topdeckapi.src.Enumerados.ROL;
import org.example.topdeckapi.src.model.Usuario;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Register → confirm the email → login → logout → the old tokens stop working. */
class AccountFlowIntegrationTest extends AbstractIntegrationTest {

    @Test
    void registerConfirmLoginLogout() throws Exception {
        String email = emailUnico();

        mvc.perform(conJson(como(post("/auth/register"), null), Map.of(
                        "nombre", "Claude Registro",
                        "email", email,
                        "password", PASSWORD,
                        "terminosAceptados", true,
                        "versionTerminosYCondicionesAceptados", "1.0")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.verificacionPendiente").value(true));

        // Not confirmed yet: right password, but no session
        mvc.perform(conJson(como(post("/auth/login"), null), Map.of("email", email, "password", PASSWORD)))
                .andExpect(status().isForbidden());

        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(emailService, atLeastOnce()).enviarConfirmacionRegistro(any(Usuario.class), link.capture());

        String tokenConfirmacion = leer(mvc.perform(conJson(como(post("/auth/confirmar-email"), null),
                        Map.of("token", link.getValue(), "password", PASSWORD)))
                .andExpect(status().isOk()).andReturn()).get("token").asText();
        mvc.perform(como(get("/user/me"), tokenConfirmacion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.rol").value("USER"));

        // The link works only once
        mvc.perform(conJson(como(post("/auth/confirmar-email"), null),
                        Map.of("token", link.getValue(), "password", PASSWORD)))
                .andExpect(status().isConflict());

        String tokenLogin = leer(mvc.perform(conJson(como(post("/auth/login"), null),
                        Map.of("email", email, "password", PASSWORD)))
                .andExpect(status().isOk()).andReturn()).get("token").asText();

        mvc.perform(como(post("/auth/logout"), tokenLogin)).andExpect(status().isNoContent());

        // Logout closes every session: both tokens are revoked and cannot be refreshed
        mvc.perform(como(get("/user/me"), tokenLogin)).andExpect(status().isUnauthorized());
        mvc.perform(como(get("/user/me"), tokenConfirmacion)).andExpect(status().isUnauthorized());
        mvc.perform(como(post("/auth/refresh"), tokenLogin)).andExpect(status().isUnauthorized());

        Usuario guardado = usuarioRepo.findByEmail(email).orElseThrow();
        assertThat(guardado.getEmailVerificado()).isTrue();
        assertThat(guardado.getTokenVerificacionHash()).isNull();
    }

    @Test
    void login_withWrongPassword_is401() throws Exception {
        String email = crearUsuario(ROL.USER).getEmail();
        mvc.perform(conJson(como(post("/auth/login"), null), Map.of("email", email, "password", "otra-cosa-123")))
                .andExpect(status().isUnauthorized());
    }
}
