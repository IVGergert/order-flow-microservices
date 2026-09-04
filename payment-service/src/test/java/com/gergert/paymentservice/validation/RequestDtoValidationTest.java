package com.gergert.paymentservice.validation;

import com.gergert.common.dto.CreatePaymentRequestDto;
import com.gergert.common.enums.PaymentMethod;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RequestDtoValidationTest {
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void createPayment_shouldRejectInvalidOrderId() {
        var request = new CreatePaymentRequestDto(0L, PaymentMethod.CARD, new BigDecimal("10.00"));
        assertThat(validator.validate(request)).anyMatch(v -> v.getPropertyPath().toString().equals("orderId"));
    }

    @Test
    void createPayment_shouldRejectNullPaymentMethod() {
        var request = new CreatePaymentRequestDto(1L, null, new BigDecimal("10.00"));
        assertThat(validator.validate(request)).anyMatch(v -> v.getPropertyPath().toString().equals("paymentMethod"));
    }

    @Test
    void createPayment_shouldRejectNonPositiveAmount() {
        var request = new CreatePaymentRequestDto(1L, PaymentMethod.CARD, BigDecimal.ZERO);
        assertThat(validator.validate(request)).anyMatch(v -> v.getPropertyPath().toString().equals("amount"));
    }

    @Test
    void createPayment_shouldAcceptValidRequest() {
        var request = new CreatePaymentRequestDto(1L, PaymentMethod.CARD, new BigDecimal("10.00"));
        assertThat(validator.validate(request)).isEmpty();
    }
}
