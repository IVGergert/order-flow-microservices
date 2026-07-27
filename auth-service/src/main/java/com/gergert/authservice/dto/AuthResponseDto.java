package com.gergert.authservice.dto;

import com.gergert.authservice.entity.Role;
import lombok.Builder;

@Builder
public record AuthResponseDto(
        String accessJwtToken,
        String refreshJwtToken,
        String tokenType,
        Long userId,
        String email,
        Role role)
{}
