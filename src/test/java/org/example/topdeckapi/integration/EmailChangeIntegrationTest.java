package org.example.topdeckapi.integration;

import org.example.topdeckapi.src.Enumerados.ROL;
import org.example.topdeckapi.src.model.Usuario;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Email change from the profile: link to the new address, sessions closed, old address told. */
class EmailChangeIntegrationTest extends AbstractIntegrationTest {

    @Test
    void changeEmail_confirmWithLink_thenLoginWithTheNewOne() throws Exception {
        Usuario u = crearUsuario(ROL.USER);
        String anterior = u.getEmail();
        String nuevo = emailUnico();
        String token = tokenDe(u);

        mvc.perform(conJson(como(post("/user/me/email"), token), Map.of("nuevoEmail", nuevo, "password", PASSWORD)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").exists());

        // Nothing changes until the link is opened
        assertThat(usuarioRepo.findByEmail(anterior)).isPresent();
        mvc.perform(como(get("/user/me"), token)).andExpect(status().isOk());

        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarConfirmacionCambioEmail(any(Usuario.class), eq(nuevo), link.capture());

        mvc.perform(conJson(como(post("/auth/confirmar-cambio-email"), null), Map.of("token", link.getValue())))
                .andExpect(status().isOk());

        Usuario guardado = usuarioRepo.findById(u.getIdUsuario()).orElseThrow();
        assertThat(guardado.getEmail()).isEqualTo(nuevo);
        assertThat(guardado.getEmailPendiente()).isNull();
        assertThat(guardado.getTokenEmailHash()).isNull();
        verify(emailService).enviarAvisoEmailCambiado(any(Usuario.class), eq(anterior));

        // Every session was closed; only the new email can sign in
        mvc.perform(como(get("/user/me"), token)).andExpect(status().isUnauthorized());
        mvc.perform(conJson(como(post("/auth/login"), null), Map.of("email", anterior, "password", PASSWORD)))
                .andExpect(status().isUnauthorized());
        mvc.perform(conJson(como(post("/auth/login"), null), Map.of("email", nuevo, "password", PASSWORD)))
                .andExpect(status().isOk());

        // The link works once
        mvc.perform(conJson(como(post("/auth/confirmar-cambio-email"), null), Map.of("token", link.getValue())))
                .andExpect(status().isConflict());
    }

    @Test
    void wrongPassword_isRejected_andNothingIsMailed() throws Exception {
        Usuario u = crearUsuario(ROL.USER);
        mvc.perform(conJson(como(post("/user/me/email"), tokenDe(u)), Map.of("nuevoEmail", emailUnico(), "password", "otra-cosa-123")))
                .andExpect(status().isConflict());
        verify(emailService, never()).enviarConfirmacionCambioEmail(any(), anyString(), anyString());
        assertThat(usuarioRepo.findById(u.getIdUsuario()).orElseThrow().getEmailPendiente()).isNull();
    }

    @Test
    void emailOfAnotherAccount_answersTheSame_butSendsNothing() throws Exception {
        Usuario u = crearUsuario(ROL.USER);
        Usuario otro = crearUsuario(ROL.USER);

        mvc.perform(conJson(como(post("/user/me/email"), tokenDe(u)), Map.of("nuevoEmail", otro.getEmail(), "password", PASSWORD)))
                .andExpect(status().isAccepted());

        verify(emailService, never()).enviarConfirmacionCambioEmail(any(), anyString(), anyString());
        Usuario guardado = usuarioRepo.findById(u.getIdUsuario()).orElseThrow();
        assertThat(guardado.getEmail()).isEqualTo(u.getEmail());
        assertThat(guardado.getEmailPendiente()).isNull();
    }

    @Test
    void sameEmail_isRejected() throws Exception {
        Usuario u = crearUsuario(ROL.USER);
        mvc.perform(conJson(como(post("/user/me/email"), tokenDe(u)), Map.of("nuevoEmail", u.getEmail(), "password", PASSWORD)))
                .andExpect(status().isConflict());
    }

    @Test
    void withoutSession_is401_andAnInvalidLinkIsRejected() throws Exception {
        mvc.perform(conJson(como(post("/user/me/email"), null), Map.of("nuevoEmail", emailUnico(), "password", PASSWORD)))
                .andExpect(status().isUnauthorized());
        mvc.perform(conJson(como(post("/auth/confirmar-cambio-email"), null), Map.of("token", "no-existe")))
                .andExpect(status().isConflict());
    }
}
