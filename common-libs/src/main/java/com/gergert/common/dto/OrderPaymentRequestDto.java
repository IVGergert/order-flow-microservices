package com.gergert.common.dto;

import com.gergert.common.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record OrderPaymentRequestDto(
        @NotNull(message = "Payment method must not be null")
        PaymentMethod paymentMethod)
{}
