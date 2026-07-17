package br.com.achadosperdidos.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

@Component
public class JwtUtil {

    private static final String TOKEN_TYPE_CLAIM = "typ";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    /** Valores de exemplo/documentação que jamais podem ir a produção (A02/A05). */
    private static final Set<String> SEGREDOS_PROIBIDOS = Set.of(
            "chave-secreta-minimo-32-chars-para-hs256",
            "troque-por-uma-chave-com-pelo-menos-32-caracteres");

    private final SecretKey secretKey;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs,
            @Value("${app.jwt.refresh.expiration-ms:2592000000}") long refreshExpirationMs) {
        validarSegredo(secret);
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    /** Aborta o boot se o segredo do JWT estiver ausente, curto demais ou for um valor de exemplo. */
    private static void validarSegredo(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET não configurado. Defina uma chave forte (>= 32 caracteres) via variável de ambiente.");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET fraco: use pelo menos 32 caracteres para HS256.");
        }
        if (SEGREDOS_PROIBIDOS.contains(secret)) {
            throw new IllegalStateException(
                    "JWT_SECRET está usando um valor de exemplo. Gere um segredo próprio antes de subir a aplicação.");
        }
    }

    public String generateAccessToken(String subject) {
        return buildToken(subject, TOKEN_TYPE_ACCESS, accessExpirationMs, null);
    }

    /** Gera um refresh token com identificador único (jti) para permitir revogação. */
    public String generateRefreshToken(String subject) {
        return buildToken(subject, TOKEN_TYPE_REFRESH, refreshExpirationMs, UUID.randomUUID().toString());
    }

    private String buildToken(String subject, String type, long expirationMs, String jti) {
        Date now = new Date();
        var builder = Jwts.builder()
                .subject(subject)
                .claim(TOKEN_TYPE_CLAIM, type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(secretKey);
        if (jti != null) {
            builder.id(jti);
        }
        return builder.compact();
    }

    public String getSubjectFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.getSubject() : null;
    }

    /** Identificador único (jti) do token — usado para revogar refresh tokens. */
    public String getJtiFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.getId() : null;
    }

    /** Data de expiração do token, como {@link java.time.LocalDateTime}. */
    public java.time.LocalDateTime getExpirationFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null || claims.getExpiration() == null) return null;
        return java.time.LocalDateTime.ofInstant(claims.getExpiration().toInstant(), java.time.ZoneId.systemDefault());
    }

    public boolean validateAccessToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) return false;
        Object typ = claims.get(TOKEN_TYPE_CLAIM);
        return typ == null || TOKEN_TYPE_ACCESS.equalsIgnoreCase(String.valueOf(typ));
    }

    public boolean validateRefreshToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) return false;
        Object typ = claims.get(TOKEN_TYPE_CLAIM);
        return typ != null && TOKEN_TYPE_REFRESH.equalsIgnoreCase(String.valueOf(typ));
    }

    private Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException | MalformedJwtException | SignatureException | IllegalArgumentException e) {
            return null;
        }
    }
}
