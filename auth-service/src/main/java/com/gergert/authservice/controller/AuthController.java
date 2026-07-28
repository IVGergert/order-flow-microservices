package com.gergert.authservice.controller;

import com.gergert.authservice.dto.AuthResponseDto;
import com.gergert.authservice.dto.LoginRequestDto;
import com.gergert.authservice.dto.RegisterRequestDto;
import com.gergert.authservice.security.jwt.JwtTokenService;
import com.gergert.authservice.service.AuthService;
import com.gergert.common.dto.jwt.JwtClaimsDto;
import com.gergert.common.security.JwtTokenValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final JwtTokenValidator jwtTokenValidator;
    private final JwtTokenService jwtTokenService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto loginDto) {

        AuthResponseDto response = authService.login(loginDto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto registerDto) {

        AuthResponseDto response = authService.register(registerDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDto> refresh(@RequestHeader("Authorization") String bearerToken) {
        String refreshToken = bearerToken.substring(7);

        if (!jwtTokenValidator.validateJwtToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!"REFRESH".equals(jwtTokenValidator.getTokenType(refreshToken))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        JwtClaimsDto claims = jwtTokenValidator.getClaimsFromToken(refreshToken);

        AuthResponseDto response = AuthResponseDto.builder()
                .accessJwtToken(jwtTokenService.generateAccessJwtToken(claims))
                .refreshJwtToken(jwtTokenService.generateRefreshJwtToken(claims))
                .tokenType("Bearer")
                .userId(claims.userId())
                .email(claims.email())
                .role(claims.role())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate")
    public ResponseEntity<JwtClaimsDto> validate(@RequestHeader("Authorization") String bearerToken) {
        String token = extractToken(bearerToken);

        if (!jwtTokenValidator.validateJwtToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!"ACCESS".equals(jwtTokenValidator.getTokenType(token))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(jwtTokenValidator.getClaimsFromToken(token));
    }

    private String extractToken(String bearerToken) {
        if (!StringUtils.hasText(bearerToken)) {
            return null;
        }

        if (bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return bearerToken;
    }
}
