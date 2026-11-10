package org.example.topdeckapi.src.service.IMPL;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.example.topdeckapi.src.model.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Access tokens last one hour and can be renewed, but never past
 * {@link #SESION_MAXIMA} from the original login ("auth_time"). Each token also
 * carries the user's session version ("sv"): logout, a password change or a role
 * change bump that version in the database and every older token stops working.
 */
@Service
public class JwtService {

    public static final Duration SESION_MAXIMA = Duration.ofDays(7);
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_SESION_VERSION = "sv";
    private static final String CLAIM_AUTH_TIME = "auth_time";

    private final Key key;
    private final long expiration;

    public JwtService(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = 3600000;
    }

    /** New session: auth_time is now. */
    public String generarToken(Usuario usuario) {
        return generarToken(usuario, Instant.now());
    }

    /** Renewal: keeps the original auth_time so the 7-day cap still applies. */
    public String generarToken(Usuario usuario, Instant authTime) {
        long ahora = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(usuario.getEmail())
                .claim(CLAIM_ROLE, "ROLE_" + usuario.getRol().name())
                .claim(CLAIM_SESION_VERSION, usuario.getSesionVersionActual())
                .claim(CLAIM_AUTH_TIME, authTime.getEpochSecond())
                .setIssuedAt(new Date(ahora))
                .setExpiration(new Date(ahora + expiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** Parsed claims, or null when the token is malformed, forged or expired. */
    public Claims leerClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public String extractEmail(String token) {
        Claims claims = leerClaims(token);
        return claims != null ? claims.getSubject() : null;
    }

    public boolean isTokenValid(String token) {
        Claims claims = leerClaims(token);
        return claims != null && claims.getSubject() != null;
    }

    /**
     * True when the (already signature-checked) claims still match the user:
     * same email, same session version, verified email and within the 7-day cap.
     */
    public boolean sesionVigente(Claims claims, Usuario usuario) {
        if (claims == null || usuario == null) return false;
        if (!usuario.getEmail().equals(claims.getSubject())) return false;
        if (!usuario.isEmailConfirmado()) return false;
        // Tokens issued before versions existed have no "sv": they match version 0.
        Number sv = claims.get(CLAIM_SESION_VERSION, Number.class);
        int versionToken = sv != null ? sv.intValue() : 0;
        if (versionToken != usuario.getSesionVersionActual()) return false;
        return authTime(claims).plus(SESION_MAXIMA).isAfter(Instant.now());
    }

    public Instant authTime(Claims claims) {
        Number authTime = claims.get(CLAIM_AUTH_TIME, Number.class);
        if (authTime != null) return Instant.ofEpochSecond(authTime.longValue());
        Date iat = claims.getIssuedAt();
        return iat != null ? iat.toInstant() : Instant.EPOCH;
    }

    public long getExpiration() {
        return expiration;
    }
}
