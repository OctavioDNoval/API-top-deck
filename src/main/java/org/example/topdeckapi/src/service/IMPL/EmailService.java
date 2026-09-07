package org.example.topdeckapi.src.service.IMPL;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.topdeckapi.src.configuracion.ContactConfig;
import org.example.topdeckapi.src.model.DetallePedido;
import org.example.topdeckapi.src.model.Pedido;
import org.example.topdeckapi.src.model.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private static final DateTimeFormatter FECHA_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", new Locale("es", "AR"));

    private static final String LOGO_PATH = "/img/logo.webp";

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final ContactConfig contactConfig;

    @Value("${spring.mail.username}")
    private String remitenteEmail;

    /*
    * Avisa al usuario que su pedido fue confirmado y se esta preparando.
    * Se llama desde PedidoService.actualizarEstado.
    * */
    public void enviarPedidoConfirmado(Usuario usuario, Pedido pedido) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("nombre", usuario.getNombre());

        buildDatosPedido(pedido, variables);
        buildDatosContacto(variables);

        variables.put("logoUrl", obtenerLogoUrl());

        enviarEmailTemplate(
                usuario.getEmail(),
                "Tu pedido " + pedido.getUuid() + " esta confirmado",
                "EmailPedidoConfirmado",
                variables);
    }

    /*
    * Avisa a los administradores que se creo un nuevo pedido.
    * Se llama desde PedidoService.guardar y guardarPedidoEfimero.
    * */
    public void enviarNuevoPedidoAdmin(List<Usuario> admins, Pedido pedido) {
        if (admins == null || admins.isEmpty()) {
            log.warn("No hay administradores registrados para notificar el nuevo pedido");
            return;
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("nombreCliente", pedido.getUsuario().getNombre());
        variables.put("emailCliente", pedido.getUsuario().getEmail());

        buildDatosPedido(pedido, variables);
        buildDatosContacto(variables);

        variables.put("logoUrl", obtenerLogoUrl());

        for (Usuario admin : admins) {
            try {
                enviarEmailTemplate(
                        admin.getEmail(),
                        "Nuevo pedido " + pedido.getUuid(),
                        "EmailNuevoPedido",
                        variables);
            } catch (Exception e) {
                log.error("No se pudo notificar al admin {}: {}", admin.getEmail(), e.getMessage());
            }
        }
    }

    private void buildDatosPedido(Pedido pedido, Map<String, Object> variables) {
        variables.put("nroPedido", pedido.getUuid());
        variables.put("fecha", pedido.getFechaPedido() != null
                ? pedido.getFechaPedido().format(FECHA_FORMATTER)
                : "-");
        variables.put("total", String.format(new Locale("es", "AR"), "$%,.2f",
                pedido.getTotal() != null ? pedido.getTotal() : 0.0));

        List<Map<String, String>> items = new ArrayList<>();
        if (pedido.getDetalles() != null) {
            for (DetallePedido detalle : pedido.getDetalles()) {
                Map<String, String> item = new HashMap<>();
                item.put("nombre", detalle.getProducto() != null ? detalle.getProducto().getNombre() : "-");
                item.put("cantidad", String.valueOf(detalle.getCantidad() != null ? detalle.getCantidad() : 0));
                item.put("precioUnitario", String.format(new Locale("es", "AR"), "$%,.2f",
                        detalle.getPrecioUnitario() != null ? detalle.getPrecioUnitario() : 0.0));
                item.put("subtotal", String.format(new Locale("es", "AR"), "$%,.2f",
                        detalle.getSubTotal() != null ? detalle.getSubTotal() : 0.0));
                items.add(item);
            }
        }
        variables.put("items", items);
    }

    private void buildDatosContacto(Map<String, Object> variables) {
        if (contactConfig != null && contactConfig.getContacto() != null) {
            variables.put("telefonoContacto", contactConfig.getContacto().getTelefono());
            variables.put("emailContacto", contactConfig.getContacto().getEmail());
            variables.put("direccion", contactConfig.getContacto().getDireccion());
            variables.put("horarioAtencion", contactConfig.getContacto().getHorario());
        } else {
            log.warn("Configuracion de contacto no disponible");
            variables.put("telefonoContacto", "N/A");
            variables.put("emailContacto", "N/A");
            variables.put("direccion", "N/A");
            variables.put("horarioAtencion", "N/A");
        }
    }

    private void enviarEmailTemplate(String destinatario, String asunto, String template, Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);

            String htmlContent = templateEngine.process(template, context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(htmlContent, true);

            String nombreRemitente = contactConfig != null
                    && contactConfig.getEmail() != null
                    && contactConfig.getEmail().getNombreRemitente() != null
                    ? contactConfig.getEmail().getNombreRemitente()
                    : "TopDeck";
            helper.setFrom(remitenteEmail, nombreRemitente);

            mailSender.send(mimeMessage);
            log.info("Mail enviado: {} -> {}", asunto, destinatario);
        } catch (Exception e) {
            log.error("Error enviando email template a {}: {}", destinatario, e.getMessage());
            throw new RuntimeException("Error enviando email", e);
        }
    }

    /*
    * Devuelve la URL absoluta del logo alojado en los resources de la API
    * (static/img/logo.webp -> /img/logo.webp), usando app.base-url.
    * Si la base URL no esta configurada, devuelve "" y el template la oculta.
    * */
    private String obtenerLogoUrl() {
        if (contactConfig != null && contactConfig.getBaseUrl() != null
                && !contactConfig.getBaseUrl().isBlank()) {
            String base = contactConfig.getBaseUrl().replaceAll("/+$", "");
            return base + LOGO_PATH;
        }
        log.warn("Base URL no configurada en app.base-url, se omite el logo en el mail");
        return "";
    }
}
