package com.gergert.authservice.service.impl;

import com.gergert.authservice.dto.CreateCourierRequestDto;
import com.gergert.authservice.dto.UserResponseDto;
import com.gergert.authservice.entity.User;
import com.gergert.authservice.exception.PasswordMismatchException;
import com.gergert.authservice.exception.UserAlreadyExistsException;
import com.gergert.authservice.kafka.CourierCreatedEventProducer;
import com.gergert.authservice.repository.UserRepository;
import com.gergert.authservice.service.AdminService;
import com.gergert.common.dto.kafka.CourierCreatedEventDto;
import com.gergert.common.enums.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CourierCreatedEventProducer courierCreatedEventProducer;

    @Override
    public UserResponseDto createCourier(CreateCourierRequestDto request) {

        log.info("Admin attempts to create courier with email: {}", request.email());

        if (!request.password().equals(request.confirmPassword())) {
            log.warn("Passwords do not match for courier with email: {}", request.email());
            throw new PasswordMismatchException("Passwords do not match");
        }

        if (userRepository.existsByEmail(request.email())) {
            log.warn("User with email already exists: {}", request.email());
            throw new UserAlreadyExistsException("User with email " + request.email() + " already exists");
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.ROLE_COURIER)
                .build();

        User savedUser = userRepository.save(user);

        CourierCreatedEventDto event = new CourierCreatedEventDto(
                savedUser.getId(),
                savedUser.getEmail(),
                request.name(),
                request.transportType()
        );

        courierCreatedEventProducer.send(event);

        log.info("CourierCreatedEvent sent successfully. User ID: {}", savedUser.getId());

        return new UserResponseDto(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole()
        );
    }
}
