package com.gergert.authservice.security.jwt;

import com.gergert.common.dto.jwt.JwtClaimsDto;
import com.gergert.common.enums.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenServiceTest {
    private static final String SECRET = "test-secret-test-secret-test-secret-123456";
    private JwtTokenService jwtTokenService;
    private SecretKey key;

    @BeforeEach
    void setUp() {
        jwtTokenService = new JwtTokenService(SECRET, 60_000, 120_000);
        key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void generateAccessJwtToken_shouldContainExpectedClaims() {
        var claims = new JwtClaimsDto(42L, "user@example.com", Role.ROLE_CUSTOMER);

        String token = jwtTokenService.generateAccessJwtToken(claims);

        var parsed = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        assertThat(parsed.getSubject()).isEqualTo("user@example.com");
        assertThat(parsed.get("userId", Long.class)).isEqualTo(42L);
        assertThat(parsed.get("role", String.class)).isEqualTo(Role.ROLE_CUSTOMER.name());
        assertThat(parsed.get("type", String.class)).isEqualTo("ACCESS");
        assertThat(parsed.getIssuedAt()).isNotNull();
        assertThat(parsed.getExpiration()).isAfter(parsed.getIssuedAt());
    }

    @Test
    void generateRefreshJwtToken_shouldContainRefreshType() {
        var claims = new JwtClaimsDto(42L, "user@example.com", Role.ROLE_CUSTOMER);

        String token = jwtTokenService.generateRefreshJwtToken(claims);

        var parsed = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        assertThat(parsed.get("type", String.class)).isEqualTo("REFRESH");
        assertThat(parsed.get("userId", Long.class)).isEqualTo(42L);
    }
}
