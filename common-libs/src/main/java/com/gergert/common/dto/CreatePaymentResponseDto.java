package com.gergert.common.dto;

import com.gergert.common.enums.PaymentMethod;
import com.gergert.common.enums.PaymentStatus;

import java.math.BigDecimal;

public record CreatePaymentResponseDto(
        Long paymentId,
        Long orderId,
        BigDecimal amount,
        PaymentStatus paymentStatus,
        PaymentMethod paymentMethod)
{}
