package com.gergert.orderservice.dto.payment;

import java.math.BigDecimal;

public record CreatePaymentResponseDto(
        Long paymentId,
        Long orderId,
        BigDecimal amount,
        String paymentMethod,
        String paymentStatus)
{}
