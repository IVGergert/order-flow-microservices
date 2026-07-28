package com.gergert.authservice.dto;

import com.gergert.common.enums.Role;
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
