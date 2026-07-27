package com.gergert.authservice.dto;

import com.gergert.authservice.entity.Role;

public record JwtClaimsDto(
        Long userId,
        String email,
        Role role)
{}
