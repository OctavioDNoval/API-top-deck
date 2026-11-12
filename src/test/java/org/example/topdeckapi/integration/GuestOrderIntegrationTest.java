package org.example.topdeckapi.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.topdeckapi.src.Enumerados.ROL;
import org.example.topdeckapi.src.model.Producto;
import org.example.topdeckapi.src.model.Usuario;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Orders without an account: shipping and pickup; stock only moves when an admin confirms. */
class GuestOrderIntegrationTest extends AbstractIntegrationTest {

    @Test
    void guestOrder_withShipping_thenAdminConfirms() throws Exception {
        Producto producto = crearProducto("1500.50", 5);
        String email = emailUnico();

        Map<String, Object> body = pedido(email, producto, 2, "ENVIO");
        body.put("direccion", Map.of(
                "direccion", "Av. Siempre Viva",
                "altura", "742",
                "ciudad", "CABA",
                "provincia", "Buenos Aires",
                "codigoPostal", "1000",
                "pais", "Argentina"));

        JsonNode pedido = leer(mvc.perform(conJson(como(post("/pedidos/public/pedidoEfimero"), null), body))
                .andExpect(status().isOk()).andReturn());

        assertThat(pedido.get("estado").asText()).isEqualTo("PENDIENTE");
        assertThat(pedido.get("tipoEntrega").asText()).isEqualTo("ENVIO");
        assertThat(pedido.get("direccion").get("direccion").asText()).isEqualTo("Av. Siempre Viva");
        // Price always comes from the database: 2 x 1500.50
        assertThat(pedido.get("total").decimalValue()).isEqualByComparingTo(new BigDecimal("3001.00"));

        Usuario guest = usuarioRepo.findByEmail(email).orElseThrow();
        assertThat(guest.getRol()).isEqualTo(ROL.GUEST);
        assertThat(guest.getPassword()).isNull();
        assertThat(stockDe(producto)).isEqualTo(5);

        String admin = tokenDe(crearUsuario(ROL.ADMIN));
        mvc.perform(conJson(como(patch("/pedidos/admin/" + pedido.get("idPedido").asText() + "/newState"), admin),
                        Map.of("estado", "CONFIRMADO")))
                .andExpect(status().isOk());
        assertThat(stockDe(producto)).isEqualTo(3);
    }

    @Test
    void guestOrder_withPickup_needsNoAddress() throws Exception {
        Producto producto = crearProducto("800.00", 3);

        JsonNode pedido = leer(mvc.perform(conJson(como(post("/pedidos/public/pedidoEfimero"), null),
                        pedido(emailUnico(), producto, 1, "RETIRO")))
                .andExpect(status().isOk()).andReturn());

        assertThat(pedido.get("tipoEntrega").asText()).isEqualTo("RETIRO");
        assertThat(pedido.get("direccion") == null || pedido.get("direccion").isNull()).isTrue();
        assertThat(pedido.get("total").decimalValue()).isEqualByComparingTo(new BigDecimal("800.00"));
    }

    @Test
    void guestOrder_withShippingButNoAddress_isRejected() throws Exception {
        Producto producto = crearProducto("800.00", 3);
        mvc.perform(conJson(como(post("/pedidos/public/pedidoEfimero"), null),
                        pedido(emailUnico(), producto, 1, "ENVIO")))
                .andExpect(status().isConflict());
    }

    @Test
    void guestOrder_withoutAcceptingTerms_isRejected() throws Exception {
        Producto producto = crearProducto("800.00", 3);
        Map<String, Object> body = pedido(emailUnico(), producto, 1, "RETIRO");
        body.put("usuario", Map.of("nombre", "Claude Invitado", "email", emailUnico(), "terminosAceptados", false));
        mvc.perform(conJson(como(post("/pedidos/public/pedidoEfimero"), null), body))
                .andExpect(status().isConflict());
    }

    @Test
    void guestOrder_overStock_isRejected() throws Exception {
        Producto producto = crearProducto("800.00", 1);
        mvc.perform(conJson(como(post("/pedidos/public/pedidoEfimero"), null),
                        pedido(emailUnico(), producto, 2, "RETIRO")))
                .andExpect(status().isConflict());
    }

    private int stockDe(Producto producto) {
        return productoRepo.findById(producto.getIdProducto()).orElseThrow().getStock();
    }

    private static Map<String, Object> pedido(String email, Producto producto, int cantidad, String tipoEntrega) {
        Map<String, Object> body = new HashMap<>();
        body.put("usuario", Map.of(
                "nombre", "Claude Invitado",
                "email", email,
                "terminosAceptados", true,
                "versionTerminosYCondicionesAceptados", "1.0"));
        body.put("tipoEntrega", tipoEntrega);
        body.put("telefonoContacto", "11 5555 0000");
        body.put("detalles", List.of(Map.of("idProducto", producto.getUuid(), "cantidad", cantidad)));
        return body;
    }
}
