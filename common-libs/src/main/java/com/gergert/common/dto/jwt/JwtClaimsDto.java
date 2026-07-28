package com.gergert.common.dto.jwt;

import com.gergert.common.enums.Role;

public record JwtClaimsDto(
        Long userId,
        String email,
        Role role)
{}
