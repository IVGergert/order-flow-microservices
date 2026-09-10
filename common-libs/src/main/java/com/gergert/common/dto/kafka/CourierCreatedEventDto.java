package com.gergert.common.dto.kafka;

public record UserCreatedEventDto (
        Long userId,
        String email,
        String name,
        String transportType)
{}
