package com.gergert.paymentservice.dto;

import com.gergert.paymentservice.entity.PaymentMethod;

import java.math.BigDecimal;

public record CreatePaymentRequestDto(
        Long orderId,
        PaymentMethod paymentMethod,
        BigDecimal amount)
{}
