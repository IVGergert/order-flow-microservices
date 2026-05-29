package com.gergert.paymentservice.dto;

import com.gergert.paymentservice.entity.PaymentMethod;
import com.gergert.paymentservice.entity.PaymentStatus;

import java.math.BigDecimal;

public record CreatePaymentResponseDto(
        Long paymentId,
        Long orderId,
        BigDecimal amount,
        PaymentStatus paymentStatus,
        PaymentMethod paymentMethod)
{}
