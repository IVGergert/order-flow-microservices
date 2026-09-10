package com.gergert.common.dto.kafka;

public record CourierCreatedEventDto(
        Long userId,
        String email,
        String name,
        String transportType)
{}
