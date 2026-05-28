package com.gergert.paymentservice.dto;

import com.gergert.paymentservice.entity.PaymentMethod;

import java.math.BigDecimal;

public record CreatePaymentResponseDto(
        Long paymentId,
        Long orderId,
        PaymentMethod paymentMethod,
        BigDecimal amount
)
{}
