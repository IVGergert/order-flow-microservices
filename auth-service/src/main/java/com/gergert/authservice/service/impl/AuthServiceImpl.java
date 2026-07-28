package com.gergert.authservice.service.impl;

import com.gergert.authservice.dto.*;
import com.gergert.authservice.entity.User;
import com.gergert.authservice.exception.PasswordMismatchException;
import com.gergert.authservice.exception.UserAlreadyExistsException;
import com.gergert.authservice.repository.UserRepository;
import com.gergert.authservice.security.jwt.JwtTokenService;
import com.gergert.authservice.service.AuthService;
import com.gergert.common.dto.jwt.JwtClaimsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;


    @Override
    public AuthResponseDto login(LoginRequestDto loginDto) {
        log.info("Login attempt for user with email: {}", loginDto.email());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDto.email(),
                        loginDto.password()
                )
        );

        log.info("User authentication successful for email: {}", loginDto.email());

        User user = userRepository.findByEmail(loginDto.email())
                .orElseThrow(() -> {
                    log.error("Authenticated user not found in database: {}", loginDto.email());
                    return new UsernameNotFoundException("User with email " + loginDto.email() + " not found");
                });

        JwtClaimsDto claims = new JwtClaimsDto(
                user.getId(),
                user.getEmail(),
                user.getRole()
        );

        log.info("User logged in successfully. User ID: {}, email: {}", user.getId(), user.getEmail());

        return buildAuthResponse(claims);
    }

    @Override
    @Transactional
    public AuthResponseDto register(RegisterRequestDto registerDto) {

        log.info("Registration attempt for user with email: {}", registerDto.email());

        if (!registerDto.password().equals(registerDto.confirmPassword())) {
            log.warn("Registration failed. Passwords do not match for email: {}", registerDto.email());
            throw new PasswordMismatchException("Passwords do not match for email: " + registerDto.email());
        }

        if (userRepository.existsByEmail(registerDto.email())) {
            log.warn("Registration failed. User with email already exists: {}", registerDto.email());
            throw new UserAlreadyExistsException("User with email " + registerDto.email() + " already exists");
        }

        User user = User.builder()
                .email(registerDto.email())
                .password(passwordEncoder.encode(registerDto.password()))
                .role(registerDto.role())
                .build();

        User savedUser = userRepository.save(user);

        log.info("User registered successfully. User ID: {}, email: {}, role: {}",
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole()
        );

        JwtClaimsDto claims = new JwtClaimsDto(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole()
        );

        return buildAuthResponse(claims);
    }

    private AuthResponseDto buildAuthResponse(JwtClaimsDto claims) {

        log.debug("Generating JWT tokens for user ID: {}", claims.userId());

        String accessToken = jwtTokenService.generateAccessJwtToken(claims);
        String refreshToken = jwtTokenService.generateRefreshJwtToken(claims);

        log.debug("JWT tokens generated successfully for user ID: {}", claims.userId());

        return AuthResponseDto.builder()
                .accessJwtToken(accessToken)
                .refreshJwtToken(refreshToken)
                .tokenType("Bearer")
                .userId(claims.userId())
                .email(claims.email())
                .role(claims.role())
                .build();
    }

}


