package com.gergert.authservice.dto;

import com.gergert.common.enums.Role;

public record UserResponseDto(
        Long id,
        String email,
        Role role)
{}
