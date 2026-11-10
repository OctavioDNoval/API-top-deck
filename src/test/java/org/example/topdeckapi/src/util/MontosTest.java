package org.example.topdeckapi.src.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MontosTest {

    @Test
    void appliesDiscountAndQuantity() {
        assertThat(Montos.subtotal(new BigDecimal("100.00"), 10, 3)).isEqualByComparingTo("270.00");
    }

    @Test
    void hasNoFloatingPointDrift() {
        // With doubles, 0.1 * 3 is 0.30000000000000004.
        assertThat(Montos.subtotal(new BigDecimal("0.10"), 0, 3)).isEqualTo(new BigDecimal("0.30"));
    }

    @Test
    void roundsHalfUpToCents() {
        // 33.33 * 85% = 28.3305
        assertThat(Montos.subtotal(new BigDecimal("33.33"), 15, 1)).isEqualTo(new BigDecimal("28.33"));
        // 0.05 * 50% = 0.025
        assertThat(Montos.subtotal(new BigDecimal("0.05"), 50, 1)).isEqualTo(new BigDecimal("0.03"));
    }

    @Test
    void treatsNullsAsZeroAndClampsDiscount() {
        assertThat(Montos.subtotal(null, 10, 2)).isEqualTo(new BigDecimal("0.00"));
        assertThat(Montos.subtotal(new BigDecimal("50"), null, null)).isEqualTo(new BigDecimal("0.00"));
        assertThat(Montos.subtotal(new BigDecimal("50"), 150, 1)).isEqualTo(new BigDecimal("0.00"));
        assertThat(Montos.subtotal(new BigDecimal("50"), -20, 1)).isEqualTo(new BigDecimal("50.00"));
    }
}
