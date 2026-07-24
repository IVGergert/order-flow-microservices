package com.gergert.paymentservice.service;

import com.gergert.common.dto.CreatePaymentRequestDto;
import com.gergert.common.dto.CreatePaymentResponseDto;

public interface PaymentService {
    CreatePaymentResponseDto makePayment(CreatePaymentRequestDto requestDto);

}
