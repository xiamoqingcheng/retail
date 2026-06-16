package com.retail.server.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    @Test
    void generateTokenIncludesSubjectAndCustomClaims() {
        String secret = "0123456789abcdef0123456789abcdef";
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret", secret);
        ReflectionTestUtils.setField(provider, "jwtExpireMinutes", 30L);

        String token = provider.generateToken("admin", Map.of("role", "ADMIN"));
        SecretKey secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        var claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();

        assertThat(claims.getSubject()).isEqualTo("admin");
        assertThat(claims.getIssuer()).isEqualTo("retail-server");
        assertThat(claims.getAudience()).containsExactly("retail");
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }
}
