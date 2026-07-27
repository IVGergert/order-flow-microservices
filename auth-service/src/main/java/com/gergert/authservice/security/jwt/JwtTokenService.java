package com.gergert.authservice.security.jwt;

import com.gergert.authservice.dto.JwtClaimsDto;
import com.gergert.authservice.entity.Role;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Service
public class JwtTokenService {
    private final SecretKey secret;
    private final Long expirationMsForJwtToken;
    private final Long expirationMsForRefreshToken;

    public JwtTokenService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms-jwt-token}") long expirationMsForJwtToken,
            @Value("${jwt.expiration-ms-refresh-token}") long expirationMsForRefreshToken) {

        this.secret = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMsForJwtToken = expirationMsForJwtToken;
        this.expirationMsForRefreshToken = expirationMsForRefreshToken;
    }

    public String generateAccessJwtToken(JwtClaimsDto dto) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expirationMsForJwtToken);

        return Jwts.builder()
                .subject(dto.email())
                .claim("userId", dto.userId())
                .claim("role", dto.role().name())
                .claim("type", "ACCESS")
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(secret)
                .compact();
    }

    public String generateRefreshJwtToken(JwtClaimsDto dto) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expirationMsForRefreshToken);

        return Jwts.builder()
                .subject(dto.email())
                .claim("userId", dto.userId())
                .claim("role", dto.role().name())
                .claim("type", "REFRESH")
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(secret)
                .compact();
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
