package com.gergert.paymentservice.service.impl;

import com.gergert.common.dto.CreatePaymentRequestDto;
import com.gergert.common.dto.CreatePaymentResponseDto;
import com.gergert.common.enums.PaymentMethod;
import com.gergert.common.enums.PaymentStatus;
import com.gergert.paymentservice.dto.PaymentMapper;
import com.gergert.paymentservice.entity.Payment;
import com.gergert.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {
    @Mock private PaymentMapper mapper;
    @Mock private PaymentRepository paymentRepository;
    @InjectMocks private PaymentServiceImpl service;

    @Test
    void makePayment_shouldReturnExistingPaymentWithoutCreatingAnother() {
        var request = new CreatePaymentRequestDto(10L, PaymentMethod.CARD, new BigDecimal("25.00"));
        var existing = new Payment(7L, 10L, new BigDecimal("25.00"), PaymentStatus.PAYMENT_SUCCEEDED, PaymentMethod.CARD);
        var response = new CreatePaymentResponseDto(7L, 10L, new BigDecimal("25.00"), PaymentStatus.PAYMENT_SUCCEEDED, PaymentMethod.CARD);
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(existing));
        when(mapper.toResponseDto(existing)).thenReturn(response);

        assertThat(service.makePayment(request)).isEqualTo(response);
        verify(mapper).toResponseDto(existing);
        verify(mapper, never()).toEntity(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void makePayment_card_shouldSaveSuccessfulPayment() {
        var request = new CreatePaymentRequestDto(10L, PaymentMethod.CARD, new BigDecimal("25.00"));
        var entity = new Payment();
        var saved = new Payment(7L, 10L, new BigDecimal("25.00"), PaymentStatus.PAYMENT_SUCCEEDED, PaymentMethod.CARD);
        var response = new CreatePaymentResponseDto(7L, 10L, new BigDecimal("25.00"), PaymentStatus.PAYMENT_SUCCEEDED, PaymentMethod.CARD);
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.empty());
        when(mapper.toEntity(request)).thenReturn(entity);
        when(paymentRepository.save(entity)).thenReturn(saved);
        when(mapper.toResponseDto(saved)).thenReturn(response);

        assertThat(service.makePayment(request)).isEqualTo(response);
        assertThat(entity.getPaymentStatus()).isEqualTo(PaymentStatus.PAYMENT_SUCCEEDED);
        verify(paymentRepository).save(entity);
    }

    @Test
    void makePayment_nonCard_shouldMarkPaymentFailed() {
        var request = new CreatePaymentRequestDto(10L, PaymentMethod.CASH, new BigDecimal("25.00"));
        var entity = new Payment();
        var saved = new Payment(7L, 10L, new BigDecimal("25.00"), PaymentStatus.PAYMENT_FAILED, PaymentMethod.CASH);
        var response = new CreatePaymentResponseDto(7L, 10L, new BigDecimal("25.00"), PaymentStatus.PAYMENT_FAILED, PaymentMethod.CASH);
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.empty());
        when(mapper.toEntity(request)).thenReturn(entity);
        when(paymentRepository.save(entity)).thenReturn(saved);
        when(mapper.toResponseDto(saved)).thenReturn(response);

        service.makePayment(request);

        assertThat(entity.getPaymentStatus()).isEqualTo(PaymentStatus.PAYMENT_FAILED);
        verify(paymentRepository).save(entity);
    }
}
