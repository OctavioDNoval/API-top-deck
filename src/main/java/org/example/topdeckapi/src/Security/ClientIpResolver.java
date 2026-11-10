package org.example.topdeckapi.src.Security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/*
* Single place that decides the client IP (rate limiting and the IP stored when
* terms are accepted).
*
* Forwarding headers are client-controlled unless a proxy we own overwrites
* them, so they are only read when app.trust-proxy=true. That is safe only if
* the API is reachable exclusively through our nginx, which sets X-Real-IP to
* the address it saw (see Front-Top-Deck/nginx.conf).
* */
@Component
public class ClientIpResolver {

    private final boolean trustProxy;

    public ClientIpResolver(@Value("${app.trust-proxy:false}") boolean trustProxy) {
        this.trustProxy = trustProxy;
    }

    public String resolve(HttpServletRequest request) {
        if (trustProxy) {
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                return realIp.trim();
            }
            // Fallback: the last entry is the one appended by the closest proxy.
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                String[] parts = forwardedFor.split(",");
                return parts[parts.length - 1].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
