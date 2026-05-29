package com.gergert.orderservice.client;

import com.gergert.orderservice.dto.payment.CreatePaymentRequestDto;
import com.gergert.orderservice.dto.payment.CreatePaymentResponseDto;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(
        url = "/api/payments",
        accept = "application/json",
        contentType = "application/json"
)

public interface PaymentHttpClient {
    @PostExchange
    CreatePaymentResponseDto createPayment(@RequestBody CreatePaymentRequestDto requestDto);
}
