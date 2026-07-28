package com.gergert.common.security;

import com.gergert.common.dto.jwt.JwtClaimsDto;
import com.gergert.common.enums.Role;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class JwtTokenValidator {

    private final SecretKey secret;

    public JwtTokenValidator(@Value("${jwt.secret}") String secretKey) {
        this.secret = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public boolean validateJwtToken(String jwtToken) {
        try {
            parseClaims(jwtToken);
            return true;

        } catch (ExpiredJwtException expException) {
            log.error("Expired JwtException", expException);
        } catch (UnsupportedJwtException expException) {
            log.error("Unsupported JwtException", expException);
        } catch (MalformedJwtException expException) {
            log.error("Malformed JwtException", expException);
        } catch (SecurityException expException) {
            log.error("Security Exception", expException);
        } catch (Exception expException) {
            log.error("Invalid jwt token", expException);
        }

        return false;
    }

    public JwtClaimsDto getClaimsFromToken(String jwtToken) {
        Claims claims = parseClaims(jwtToken);

        return new JwtClaimsDto(
                claims.get("userId", Long.class),
                claims.getSubject(),
                Role.valueOf(claims.get("role", String.class))
        );
    }

    public String getTokenType(String jwtToken) {
        return parseClaims(jwtToken).get("type", String.class);
    }

    private Claims parseClaims(String jwtToken) {
        return Jwts.parser()
                .verifyWith(secret)
                .build()
                .parseSignedClaims(jwtToken)
                .getPayload();
    }
}
