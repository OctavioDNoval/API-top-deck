package org.example.topdeckapi.src.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class AuditMensajeTest {

    @Test
    void dineroEnFormatoArgentino() {
        assertThat(AuditMensaje.dinero(new BigDecimal("8500"))).isEqualTo("$8.500,00");
        assertThat(AuditMensaje.dinero(new BigDecimal("1234567.5"))).isEqualTo("$1.234.567,50");
        assertThat(AuditMensaje.dinero(null)).isEqualTo("$0,00");
    }

    @Test
    void codigoCortoDelPedido() {
        assertThat(AuditMensaje.codigo("a1b2c3d4-e5f6-7890")).isEqualTo("#A1B2C3D4");
    }

    @Test
    void cambiosSoloListaLoQueCambio() {
        String texto = new AuditMensaje.Cambios()
                .valor("nombre", "Sobre", "Sobre")
                .monto("precio", new BigDecimal("8500.00"), new BigDecimal("9000"))
                .monto("costo", new BigDecimal("10.0"), new BigDecimal("10.00")) // same amount, different scale
                .valor("stock", 50, 45)
                .campo("descripción", "vieja", "nueva")
                .campo("imagen", "a.png", "a.png")
                .nota(true, "contraseña")
                .texto("Edición del producto \"Sobre\"");

        assertThat(texto).isEqualTo(
                "Edición del producto \"Sobre\": precio $8.500,00 → $9.000,00, stock 50 → 45, descripción, contraseña");
    }

    @Test
    void cambiosSinNadaQueMostrar() {
        assertThat(new AuditMensaje.Cambios().valor("nombre", "A", "A").texto("Edición del tag \"A\""))
                .isEqualTo("Edición del tag \"A\" (sin cambios)");
    }

    @Test
    void fechasHorasYVacios() {
        String texto = new AuditMensaje.Cambios()
                .valor("fecha", LocalDate.of(2026, 12, 20), LocalDate.of(2026, 12, 24))
                .valor("hora", LocalTime.of(18, 0), LocalTime.of(19, 30))
                .valor("tag", null, "Pokémon")
                .texto("Edición");
        assertThat(texto).isEqualTo("Edición: fecha 20/12/2026 → 24/12/2026, hora 18:00 h → 19:30 h, tag — → Pokémon");
    }

    @Test
    void plural() {
        assertThat(AuditMensaje.plural(1, "ítem", "ítems")).isEqualTo("1 ítem");
        assertThat(AuditMensaje.plural(3, "ítem", "ítems")).isEqualTo("3 ítems");
    }
}
