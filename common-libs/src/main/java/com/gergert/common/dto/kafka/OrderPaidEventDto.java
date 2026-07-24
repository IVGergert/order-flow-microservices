package com.gergert.common.dto.kafka;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record OrderPaidEventDto(
        Long orderId,
        String address,
        BigDecimal amount)
{}
