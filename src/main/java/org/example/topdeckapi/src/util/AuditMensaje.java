package org.example.topdeckapi.src.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/*
 * Readable messages for the audit log (auditoria.detalle), so every entity
 * describes what happened the same way. Personal data (names, emails, phones,
 * addresses of customers) never goes in here: user and address messages only
 * say which fields changed, not their values.
 */
public final class AuditMensaje {

    private static final Locale ES_AR = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private AuditMensaje() {
    }

    /** "$8.500,00" (es-AR), "$0,00" for null. */
    public static String dinero(BigDecimal monto) {
        NumberFormat f = NumberFormat.getNumberInstance(ES_AR);
        f.setMinimumFractionDigits(2);
        f.setMaximumFractionDigits(2);
        return "$" + f.format(monto != null ? monto : BigDecimal.ZERO);
    }

    /** Short order code as the admin panel shows it: "#A1B2C3D4". */
    public static String codigo(String uuid) {
        return uuid == null ? "#?" : "#" + uuid.substring(0, Math.min(8, uuid.length())).toUpperCase();
    }

    public static String fecha(LocalDate fecha) {
        return fecha == null ? "sin fecha" : fecha.format(FECHA);
    }

    public static String hora(LocalTime hora) {
        return hora == null ? "" : hora.format(HORA) + " h";
    }

    public static String plural(int n, String singular, String plural) {
        return n + " " + (n == 1 ? singular : plural);
    }

    /**
     * Collects the fields that really changed in an edit:
     * new Cambios().valor("precio", a, b).texto("Edición de X") -> "Edición de X: precio $1 → $2".
     */
    public static final class Cambios {
        private final List<String> partes = new ArrayList<>();

        /** Shows both values: "stock 50 → 45". */
        public Cambios valor(String campo, Object antes, Object despues) {
            if (distinto(antes, despues)) {
                partes.add(campo + " " + mostrar(antes) + " → " + mostrar(despues));
            }
            return this;
        }

        public Cambios monto(String campo, BigDecimal antes, BigDecimal despues) {
            if (distinto(antes, despues)) {
                partes.add(campo + " " + dinero(antes) + " → " + dinero(despues));
            }
            return this;
        }

        /** Only says the field changed (long texts, images, personal data). */
        public Cambios campo(String campo, Object antes, Object despues) {
            if (distinto(antes, despues)) {
                partes.add(campo);
            }
            return this;
        }

        /** Adds a fixed note when the condition holds ("contraseña", "sesiones cerradas"). */
        public Cambios nota(boolean condicion, String texto) {
            if (condicion) {
                partes.add(texto);
            }
            return this;
        }

        public boolean vacio() {
            return partes.isEmpty();
        }

        public String texto(String prefijo) {
            return vacio() ? prefijo + " (sin cambios)" : prefijo + ": " + String.join(", ", partes);
        }

        private static boolean distinto(Object a, Object b) {
            if (a instanceof BigDecimal x && b instanceof BigDecimal y) {
                return x.compareTo(y) != 0;
            }
            return !Objects.equals(a, b);
        }

        private static String mostrar(Object v) {
            if (v == null || (v instanceof String s && s.isBlank())) return "—";
            if (v instanceof LocalDate d) return fecha(d);
            if (v instanceof LocalTime t) return hora(t);
            return v.toString();
        }
    }
}
