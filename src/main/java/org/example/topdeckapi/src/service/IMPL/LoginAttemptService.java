package org.example.topdeckapi.src.service.IMPL;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-account brute-force protection: after {@link #MAX_FALLOS} wrong passwords
 * within {@link #VENTANA}, the email is locked for {@link #BLOQUEO}. It complements
 * the per-IP rate limit (an attacker with many IPs still hits this). Emails are
 * tracked whether or not the account exists, so a lock never reveals that.
 * State is in memory: a restart clears it, which is acceptable for a single node.
 */
@Service
public class LoginAttemptService {

    static final int MAX_FALLOS = 5;
    static final Duration VENTANA = Duration.ofMinutes(15);
    static final Duration BLOQUEO = Duration.ofMinutes(15);
    private static final int MAX_ENTRADAS = 50_000;

    private record Estado(int fallos, Instant desde, Instant bloqueadoHasta) {}

    private final Map<String, Estado> estados = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginAttemptService() {
        this(Clock.systemUTC());
    }

    LoginAttemptService(Clock clock) {
        this.clock = clock;
    }

    private static String clave(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /** Minutes left of the lock, or 0 when the email may try to log in. */
    public long minutosBloqueado(String email) {
        Estado e = estados.get(clave(email));
        Instant ahora = clock.instant();
        if (e == null || e.bloqueadoHasta() == null || !e.bloqueadoHasta().isAfter(ahora)) return 0;
        return Math.max(1, Duration.between(ahora, e.bloqueadoHasta()).toMinutes() + 1);
    }

    public void registrarFallo(String email) {
        if (estados.size() > MAX_ENTRADAS) limpiarVencidos();
        Instant ahora = clock.instant();
        estados.compute(clave(email), (k, e) -> {
            if (e == null || e.desde().plus(VENTANA).isBefore(ahora)) {
                return new Estado(1, ahora, null);
            }
            int fallos = e.fallos() + 1;
            return new Estado(fallos, e.desde(), fallos >= MAX_FALLOS ? ahora.plus(BLOQUEO) : e.bloqueadoHasta());
        });
    }

    public void registrarExito(String email) {
        estados.remove(clave(email));
    }

    private void limpiarVencidos() {
        Instant ahora = clock.instant();
        estados.entrySet().removeIf(en -> {
            Estado e = en.getValue();
            boolean ventanaVencida = e.desde().plus(VENTANA).isBefore(ahora);
            boolean sinBloqueo = e.bloqueadoHasta() == null || e.bloqueadoHasta().isBefore(ahora);
            return ventanaVencida && sinBloqueo;
        });
    }
}
