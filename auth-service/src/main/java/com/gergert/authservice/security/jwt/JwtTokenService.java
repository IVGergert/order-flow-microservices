package com.gergert.authservice.security.jwt;


import com.gergert.common.dto.jwt.JwtClaimsDto;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
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
}
