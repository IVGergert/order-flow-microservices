package com.gergert.authservice.service.impl;

import com.gergert.authservice.dto.*;
import com.gergert.authservice.entity.User;
import com.gergert.authservice.exception.PasswordMismatchException;
import com.gergert.authservice.exception.UserAlreadyExistsException;
import com.gergert.authservice.repository.UserRepository;
import com.gergert.authservice.security.jwt.JwtTokenService;
import com.gergert.authservice.service.AuthService;
import com.gergert.common.dto.jwt.JwtClaimsDto;
import com.gergert.common.enums.Role;
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

        User user = userRepository.findByEmail(loginDto.email())
                .orElseThrow(() -> {
                    log.error("Authenticated user not found in database: {}", loginDto.email());
                    return new UsernameNotFoundException("User with email " + loginDto.email() + " not found");
                });

        log.info("User logged in successfully. User ID: {}, email: {}", user.getId(), user.getEmail());

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponseDto register(RegisterRequestDto registerDto) {
        log.info("User registration attempt for email: {}", registerDto.email());

        validateRegistration(registerDto);

        User savedUser = createUser(registerDto, Role.ROLE_CUSTOMER);
        log.info("Customer registered successfully. User ID: {}", savedUser.getId());

        return buildAuthResponse(savedUser);
    }

    private void validateRegistration(RegisterRequestDto registerDto) {
        if (!registerDto.password().equals(registerDto.confirmPassword())) {
            log.warn("Passwords do not match for email: {}", registerDto.email());
            throw new PasswordMismatchException("Passwords do not match");
        }

        if (userRepository.existsByEmail(registerDto.email())) {
            log.warn("User with email already exists: {}", registerDto.email());
            throw new UserAlreadyExistsException("User with email " + registerDto.email() + " already exists");
        }
    }

    private User createUser(RegisterRequestDto registerDto, Role role) {
        User user = User.builder()
                .email(registerDto.email())
                .password(passwordEncoder.encode(registerDto.password()))
                .role(role)
                .build();

        return userRepository.save(user);
    }

    private AuthResponseDto buildAuthResponse(User user) {
        JwtClaimsDto claims = new JwtClaimsDto(
                user.getId(),
                user.getEmail(),
                user.getRole()
        );

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


