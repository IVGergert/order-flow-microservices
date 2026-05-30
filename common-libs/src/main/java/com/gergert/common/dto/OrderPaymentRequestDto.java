package com.gergert.common.dto;

import com.gergert.common.enums.PaymentMethod;

public record OrderPaymentRequestDto(
        PaymentMethod paymentMethod)
{}
