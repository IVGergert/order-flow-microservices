package com.gergert.authservice.service.impl;

import com.gergert.authservice.dto.AuthResponseDto;
import com.gergert.authservice.dto.LoginRequestDto;
import com.gergert.authservice.dto.RegisterRequestDto;
import com.gergert.authservice.entity.User;
import com.gergert.authservice.exception.PasswordMismatchException;
import com.gergert.authservice.exception.UserAlreadyExistsException;
import com.gergert.authservice.repository.UserRepository;
import com.gergert.authservice.security.jwt.JwtTokenService;
import com.gergert.common.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenService jwtTokenService;
    @InjectMocks private AuthServiceImpl authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("user@example.com")
                .password("encoded-password")
                .role(Role.ROLE_CUSTOMER)
                .build();
    }

    @Test
    void register_shouldCreateCustomerAndReturnTokens() {
        var request = new RegisterRequestDto("user@example.com", "password", "password");
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtTokenService.generateAccessJwtToken(any())).thenReturn("access-token");
        when(jwtTokenService.generateRefreshJwtToken(any())).thenReturn("refresh-token");

        AuthResponseDto response = authService.register(request);

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.role()).isEqualTo(Role.ROLE_CUSTOMER);
        assertThat(response.accessJwtToken()).isEqualTo("access-token");
        assertThat(response.refreshJwtToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("encoded-password");
        assertThat(captor.getValue().getRole()).isEqualTo(Role.ROLE_CUSTOMER);
    }

    @Test
    void register_shouldRejectWhenPasswordsDoNotMatch() {
        var request = new RegisterRequestDto("user@example.com", "password", "different");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(PasswordMismatchException.class);

        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldRejectExistingEmail() {
        var request = new RegisterRequestDto("user@example.com", "password", "password");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class);

        verify(userRepository).existsByEmail(request.email());
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void login_shouldAuthenticateAndReturnTokens() {
        var request = new LoginRequestDto("user@example.com", "password");
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(jwtTokenService.generateAccessJwtToken(any())).thenReturn("access-token");
        when(jwtTokenService.generateRefreshJwtToken(any())).thenReturn("refresh-token");

        AuthResponseDto response = authService.login(request);

        assertThat(response.email()).isEqualTo(user.getEmail());
        assertThat(response.userId()).isEqualTo(user.getId());
        assertThat(response.role()).isEqualTo(user.getRole());
        verify(authenticationManager).authenticate(any());
        verify(userRepository).findByEmail(request.email());
    }

    @Test
    void login_shouldPropagateAuthenticationFailure() {
        var request = new LoginRequestDto("user@example.com", "wrong-password");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(userRepository, never()).findByEmail(anyString());
        verify(jwtTokenService, never()).generateAccessJwtToken(any());
    }

    @Test
    void login_shouldThrowWhenAuthenticatedUserIsMissing() {
        var request = new LoginRequestDto("user@example.com", "password");
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UsernameNotFoundException.class);

        verify(authenticationManager).authenticate(any());
        verify(jwtTokenService, never()).generateAccessJwtToken(any());
    }
}
