package org.example.topdeckapi.src.Security;

import jakarta.annotation.PreDestroy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/*
* Fixed-window rate limiting per client IP and per rule. State is in memory,
* which is enough for a single API instance; several instances would need a
* shared store (e.g. Redis).
* */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private record Rule(String name, String method, String pathPrefix, int maxRequests, Duration window, String message) {
        boolean matches(HttpServletRequest request) {
            return (method == null || method.equalsIgnoreCase(request.getMethod()))
                    && request.getRequestURI().startsWith(pathPrefix);
        }
    }

    private static final class Window {
        private final long startMillis;
        private int count;

        private Window(long startMillis) {
            this.startMillis = startMillis;
        }
    }

    private static final List<Rule> RULES = List.of(
            new Rule("auth", null, "/auth/", 10, Duration.ofMinutes(1),
                    "Demasiadas solicitudes. Intente de nuevo en un minuto."),
            // Public order endpoint: each order also emails every admin.
            new Rule("pedido-efimero", "POST", "/pedidos/public/pedidoEfimero", 5, Duration.ofMinutes(10),
                    "Demasiados pedidos seguidos. Intente de nuevo en unos minutos.")
    );

    private static final Duration MAX_WINDOW = RULES.stream()
            .map(Rule::window)
            .max(Duration::compareTo)
            .orElse(Duration.ofMinutes(1));

    private final ClientIpResolver clientIpResolver;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "rate-limit-cleaner");
        t.setDaemon(true);
        return t;
    });

    public RateLimitFilter(ClientIpResolver clientIpResolver) {
        this.clientIpResolver = clientIpResolver;
        cleaner.scheduleAtFixedRate(this::evictExpired, 1, 1, TimeUnit.MINUTES);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Rule rule = RULES.stream().filter(r -> r.matches(request)).findFirst().orElse(null);
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = rule.name() + "|" + clientIpResolver.resolve(request);
        long now = System.currentTimeMillis();
        long windowMillis = rule.window().toMillis();

        Window window = windows.compute(key, (k, current) ->
                current == null || now - current.startMillis >= windowMillis ? new Window(now) : current);

        int count;
        synchronized (window) {
            count = ++window.count;
        }

        if (count > rule.maxRequests()) {
            long retryAfterSeconds = Math.max(1, (window.startMillis + windowMillis - now) / 1000);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"status\":429,\"message\":\"" + rule.message() + "\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void evictExpired() {
        long cutoff = System.currentTimeMillis() - MAX_WINDOW.toMillis();
        windows.values().removeIf(w -> w.startMillis < cutoff);
    }

    @PreDestroy
    void shutdown() {
        cleaner.shutdownNow();
    }
}
