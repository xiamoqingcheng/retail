package com.retail.server.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final String ISSUER = "retail-server";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expire-minutes:120}")
    private long jwtExpireMinutes;

    public String generateToken(String subject, Map<String, Object> claims) {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + Duration.ofMinutes(jwtExpireMinutes).toMillis());
        SecretKey secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        var builder = Jwts.builder()
                .subject(subject)
                .issuer(ISSUER)
                .claim("aud", "retail")
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expireAt);
        if (claims != null) {
            claims.forEach(builder::claim);
        }
        return builder.signWith(secretKey).compact();
    }
}