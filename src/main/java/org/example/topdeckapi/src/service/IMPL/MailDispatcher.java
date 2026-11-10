package org.example.topdeckapi.src.service.IMPL;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/*
* Sends already-rendered messages on the bounded "mailExecutor" pool
* (configuracion/AsyncConfig). Failures are logged, never propagated.
* */
@Component
@RequiredArgsConstructor
@Slf4j
public class MailDispatcher {

    private final JavaMailSender mailSender;

    @Async("mailExecutor")
    public void enviar(MimeMessage mensaje, String asunto, String destinatario) {
        try {
            mailSender.send(mensaje);
            log.info("Mail enviado: {} -> {}", asunto, destinatario);
        } catch (Exception e) {
            log.error("Error enviando mail '{}' a {}: {}", asunto, destinatario, e.getMessage());
        }
    }
}
