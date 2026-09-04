package com.gergert.paymentservice.service.impl;

import com.gergert.common.dto.CreatePaymentRequestDto;
import com.gergert.common.dto.CreatePaymentResponseDto;
import com.gergert.common.enums.PaymentMethod;
import com.gergert.common.enums.PaymentStatus;
import com.gergert.paymentservice.dto.PaymentMapper;
import com.gergert.paymentservice.repository.PaymentRepository;
import com.gergert.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentMapper mapper;
    private final PaymentRepository paymentRepository;

    @Override
    public CreatePaymentResponseDto makePayment(CreatePaymentRequestDto requestDto){
        var found = paymentRepository.findByOrderId(requestDto.orderId());

        if (found.isPresent()){
            log.info("Payment already exists for orderId={}", requestDto.orderId());
            return mapper.toResponseDto(found.get());
        }

        var entity = mapper.toEntity(requestDto);

        var status = requestDto.paymentMethod().equals(PaymentMethod.CARD)
                ? PaymentStatus.PAYMENT_SUCCEEDED
                : PaymentStatus.PAYMENT_FAILED;

        entity.setPaymentStatus(status);

        var savedEntity = paymentRepository.save(entity);
        return mapper.toResponseDto(savedEntity);
    }
}
