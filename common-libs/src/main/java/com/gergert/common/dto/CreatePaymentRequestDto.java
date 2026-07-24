package com.gergert.common.dto;

import com.gergert.common.enums.PaymentMethod;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CreatePaymentRequestDto(
        Long orderId,
        PaymentMethod paymentMethod,
        BigDecimal amount)
{}
