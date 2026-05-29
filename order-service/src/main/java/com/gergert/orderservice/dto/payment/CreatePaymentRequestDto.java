package com.gergert.orderservice.dto.payment;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CreatePaymentRequestDto(
        Long orderId,
        String paymentMethod,
        BigDecimal amount)
{}
