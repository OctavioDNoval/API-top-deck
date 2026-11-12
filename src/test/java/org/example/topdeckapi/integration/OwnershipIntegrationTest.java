package org.example.topdeckapi.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.topdeckapi.src.Enumerados.ROL;
import org.example.topdeckapi.src.model.Producto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** User B can never read or change the cart, addresses or orders of user A. */
class OwnershipIntegrationTest extends AbstractIntegrationTest {

    private String tokenA;
    private String tokenB;
    private Producto producto;

    @BeforeEach
    void setUp() {
        tokenA = tokenDe(crearUsuario(ROL.USER));
        tokenB = tokenDe(crearUsuario(ROL.USER));
        producto = crearProducto("1000.00", 10);
    }

    @Test
    void addressOfA_cannotBeEditedDeletedOrUsedByB() throws Exception {
        String idDireccion = crearDireccion(tokenA);

        mvc.perform(conJson(como(patch("/direccion/user/" + idDireccion), tokenB), direccion("Calle de B")))
                .andExpect(status().isForbidden());
        mvc.perform(como(patch("/direccion/user/" + idDireccion + "/principal"), tokenB))
                .andExpect(status().isForbidden());
        mvc.perform(como(delete("/direccion/user/" + idDireccion), tokenB))
                .andExpect(status().isForbidden());
        mvc.perform(conJson(como(post("/pedidos/user/newPedido"), tokenB), Map.of(
                        "tipoEntrega", "ENVIO",
                        "idDireccion", idDireccion,
                        "telefonoContacto", "11 5555 0000",
                        "detalles", List.of(Map.of("idProducto", producto.getUuid(), "cantidad", 1)))))
                .andExpect(status().isForbidden());

        // B does not see it and A still has it untouched
        JsonNode deB = leer(mvc.perform(como(get("/direccion/user/getAll"), tokenB)).andReturn());
        assertThat(deB.findValuesAsText("idDireccion")).doesNotContain(idDireccion);
        JsonNode deA = leer(mvc.perform(como(get("/direccion/user/getAll"), tokenA)).andReturn());
        assertThat(deA.findValuesAsText("direccion")).containsExactly("Calle de A");
    }

    @Test
    void cartOfA_cannotBeReadOrChangedByB() throws Exception {
        JsonNode detalle = leer(mvc.perform(conJson(como(post("/carrito/user/agregarDetalle"), tokenA),
                        Map.of("idProducto", producto.getUuid(), "cantidad", 2)))
                .andExpect(status().isCreated()).andReturn());
        String idDetalle = detalle.get("idDetalleCarrito").asText();
        String idCarrito = leer(mvc.perform(como(get("/carrito/user/getCarrito"), tokenA)).andReturn())
                .get("idCarrito").asText();

        mvc.perform(como(get("/carrito/user/" + idCarrito + "/detalles"), tokenB))
                .andExpect(status().isForbidden());
        mvc.perform(como(patch("/carrito/user/detalle/actualizar/" + idDetalle).param("nuevaCantidad", "5"), tokenB))
                .andExpect(status().isForbidden());
        mvc.perform(como(delete("/carrito/user/detalle/" + idDetalle), tokenB))
                .andExpect(status().isForbidden());
        mvc.perform(como(delete("/carrito/user/" + idCarrito + "/empty"), tokenB))
                .andExpect(status().isForbidden());

        JsonNode detallesA = leer(mvc.perform(como(get("/carrito/user/" + idCarrito + "/detalles"), tokenA))
                .andExpect(status().isOk()).andReturn());
        assertThat(detallesA).hasSize(1);
        assertThat(detallesA.get(0).get("cantidad").asInt()).isEqualTo(2);
    }

    @Test
    void ordersOfA_areNotListedForB() throws Exception {
        JsonNode pedido = leer(mvc.perform(conJson(como(post("/pedidos/user/newPedido"), tokenA), Map.of(
                        "tipoEntrega", "RETIRO",
                        "telefonoContacto", "11 5555 0000",
                        "detalles", List.of(Map.of("idProducto", producto.getUuid(), "cantidad", 1)))))
                .andExpect(status().isOk()).andReturn());
        String idPedido = pedido.get("idPedido").asText();

        JsonNode deA = leer(mvc.perform(como(get("/pedidos/user/pedidos"), tokenA)).andReturn());
        assertThat(deA).extracting(p -> p.get("idPedido").asText()).containsExactly(idPedido);
        JsonNode deB = leer(mvc.perform(como(get("/pedidos/user/pedidos"), tokenB)).andReturn());
        assertThat(deB).isEmpty();
        // The single-order endpoints are admin only
        mvc.perform(como(get("/pedidos/admin/" + idPedido), tokenB)).andExpect(status().isForbidden());
    }

    private String crearDireccion(String token) throws Exception {
        JsonNode d = leer(mvc.perform(conJson(como(post("/direccion/user/save"), token), direccion("Calle de A")))
                .andExpect(status().isOk()).andReturn());
        return d.get("idDireccion").asText();
    }

    private static Map<String, Object> direccion(String calle) {
        return Map.of(
                "direccion", calle,
                "altura", "123",
                "ciudad", "CABA",
                "provincia", "Buenos Aires",
                "codigoPostal", "1000",
                "pais", "Argentina");
    }
}
