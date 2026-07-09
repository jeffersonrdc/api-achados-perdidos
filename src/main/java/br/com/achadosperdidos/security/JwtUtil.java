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

@Component
public class JwtUtil {

    private static final String TOKEN_TYPE_CLAIM = "typ";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private final SecretKey secretKey;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs,
            @Value("${app.jwt.refresh.expiration-ms:2592000000}") long refreshExpirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generateAccessToken(String subject) {
        return buildToken(subject, TOKEN_TYPE_ACCESS, accessExpirationMs);
    }

    public String generateRefreshToken(String subject) {
        return buildToken(subject, TOKEN_TYPE_REFRESH, refreshExpirationMs);
    }

    private String buildToken(String subject, String type, long expirationMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .claim(TOKEN_TYPE_CLAIM, type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    public String getSubjectFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.getSubject() : null;
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
