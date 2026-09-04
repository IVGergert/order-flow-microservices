package com.gergert.common.dto;

import com.gergert.common.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CreatePaymentRequestDto(
        @NotNull(message = "Order ID must not be null")
        @Positive(message = "Order ID must be positive")
        Long orderId,

        @NotNull(message = "Payment method must not be null")
        PaymentMethod paymentMethod,

        @NotNull(message = "Amount must not be null")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        BigDecimal amount
)
{}
