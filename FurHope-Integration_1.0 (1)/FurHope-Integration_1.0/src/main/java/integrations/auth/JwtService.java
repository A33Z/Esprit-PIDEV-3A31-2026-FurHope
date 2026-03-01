package integrations.auth;

import entities.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Properties;

public class JwtService {

    private static final String DEFAULT_SECRET = "furhope-default-dev-secret-change-me-please-2026";
    private final SecretKey signingKey;
    private final long expirationHours;

    public JwtService() {
        Properties properties = loadProperties();
        String secret = firstNonBlank(
                System.getenv("APP_JWT_SECRET"),
                properties.getProperty("app.jwt.secret"),
                DEFAULT_SECRET
        );
        String expiration = firstNonBlank(
                System.getenv("APP_JWT_EXPIRATION_HOURS"),
                properties.getProperty("app.jwt.expiration_hours"),
                "8"
        );
        this.signingKey = Keys.hmacShaKeyFor(fixSecretLength(secret).getBytes(StandardCharsets.UTF_8));
        this.expirationHours = parseLong(expiration, 8L);
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expires = now.plus(expirationHours, ChronoUnit.HOURS);

        return Jwts.builder()
                .subject(user.getEmail() == null ? String.valueOf(user.getId()) : user.getEmail())
                .claim("uid", user.getId())
                .claim("role", safe(user.getRole()))
                .claim("firstName", safe(user.getFirstName()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expires))
                .signWith(signingKey)
                .compact();
    }

    public boolean isTokenValid(String token) {
        try {
            Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = JwtService.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (Exception ignored) {
        }
        return properties;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private String fixSecretLength(String secret) {
        if (secret == null) {
            secret = DEFAULT_SECRET;
        }
        if (secret.length() >= 32) {
            return secret;
        }
        StringBuilder sb = new StringBuilder(secret);
        while (sb.length() < 32) {
            sb.append('x');
        }
        return sb.toString();
    }

    private long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
