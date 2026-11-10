package org.example.topdeckapi.src.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/*
* Money math in one place (cart total and order subtotals must agree).
* Amounts are BigDecimal with 2 decimals, rounded HALF_UP.
* */
public final class Montos {

    public static final int ESCALA = 2;
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private Montos() {
    }

    /** precio * cantidad * (100 - descuento%) / 100, rounded to cents. */
    public static BigDecimal subtotal(BigDecimal precio, Integer descuentoPorcentaje, Integer cantidad) {
        BigDecimal p = precio != null ? precio : BigDecimal.ZERO;
        int cant = cantidad != null ? cantidad : 0;
        int desc = descuentoPorcentaje != null ? Math.max(0, Math.min(100, descuentoPorcentaje)) : 0;
        return p.multiply(BigDecimal.valueOf(cant))
                .multiply(BigDecimal.valueOf(100L - desc))
                .divide(CIEN, ESCALA, RoundingMode.HALF_UP);
    }

    public static BigDecimal redondear(BigDecimal monto) {
        return (monto != null ? monto : BigDecimal.ZERO).setScale(ESCALA, RoundingMode.HALF_UP);
    }
}
