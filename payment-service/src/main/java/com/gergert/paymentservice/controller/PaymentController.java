package com.gergert.paymentservice.controller;

import com.gergert.common.dto.CreatePaymentRequestDto;
import com.gergert.common.dto.CreatePaymentResponseDto;
import com.gergert.common.dto.jwt.JwtClaimsDto;
import com.gergert.paymentservice.service.PaymentService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping
    public CreatePaymentResponseDto createPayment(@RequestBody CreatePaymentRequestDto requestDto,
                                                  @AuthenticationPrincipal JwtClaimsDto claims){

        log.info("Processing payment for orderId={}, amount={} initiated by userId={}, email={}",
                requestDto.orderId(),
                requestDto.amount(),
                claims.userId(),
                claims.email());

        return paymentService.makePayment(requestDto);
    }
}
