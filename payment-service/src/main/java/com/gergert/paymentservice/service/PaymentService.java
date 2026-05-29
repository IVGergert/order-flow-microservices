package com.gergert.paymentservice.service;

import com.gergert.paymentservice.dto.CreatePaymentRequestDto;
import com.gergert.paymentservice.dto.CreatePaymentResponseDto;
import com.gergert.paymentservice.dto.PaymentMapper;
import com.gergert.paymentservice.entity.PaymentMethod;
import com.gergert.paymentservice.entity.PaymentStatus;
import com.gergert.paymentservice.repository.PaymentRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class PaymentService {

    private final PaymentMapper mapper;
    private final PaymentRepository paymentRepository;

    public CreatePaymentResponseDto makePayment(CreatePaymentRequestDto requestDto){
        var found = paymentRepository.findByOrderId(requestDto.orderId());

        if (found.isPresent()){
            log.info("Payment already exists for orderId={}", requestDto.orderId());
            return mapper.toResponseDto(found.get());
        }

        var entity = mapper.toEntity(requestDto);

        var status = requestDto.paymentMethod().equals(PaymentMethod.QR)
                ? PaymentStatus.PAYMENT_FAILED
                : PaymentStatus.PAYMENT_SUCCEEDED;

        entity.setPaymentStatus(status);

        var savedEntity = paymentRepository.save(entity);
        return mapper.toResponseDto(savedEntity);
    }
}
