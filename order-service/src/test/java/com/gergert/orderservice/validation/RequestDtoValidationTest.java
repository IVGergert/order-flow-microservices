package com.gergert.orderservice.validation;

import com.gergert.orderservice.dto.CreateOrderRequestDto;
import com.gergert.orderservice.dto.OrderItemRequestDto;
import com.gergert.common.dto.OrderPaymentRequestDto;
import com.gergert.common.enums.PaymentMethod;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RequestDtoValidationTest {
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation
                .buildDefaultValidatorFactory()
                .getValidator();
    }

    // CreateOrderRequestDto

    @Test
    void createOrder_shouldRejectBlankAddress() {
        CreateOrderRequestDto request =
                new CreateOrderRequestDto(
                        " ",
                        Set.of(new OrderItemRequestDto(1L, 1)));

        assertThat(validator.validate(request))
                .anyMatch(error ->
                        error.getPropertyPath()
                                .toString()
                                .equals("address"));
    }

    @Test
    void createOrder_shouldRejectEmptyItems() {
        CreateOrderRequestDto request =
                new CreateOrderRequestDto(
                        "Test address", Set.of());

        assertThat(validator.validate(request))
                .anyMatch(error ->
                        error.getPropertyPath()
                                .toString()
                                .equals("items"));
    }

    @Test
    void createOrder_shouldValidateNestedItems() {
        CreateOrderRequestDto request =
                new CreateOrderRequestDto(
                        "Test address",
                        Set.of(new OrderItemRequestDto(-1L, 0)));

        var fields = validator.validate(request)
                .stream()
                .map(error ->
                        error.getPropertyPath().toString()
                )
                .toList();

        assertThat(fields)
                .anyMatch(path -> path.contains("itemId"));

        assertThat(fields)
                .anyMatch(path -> path.contains("quantity"));
    }

    @Test
    void createOrder_shouldAcceptValidRequest() {
        CreateOrderRequestDto request =
                new CreateOrderRequestDto(
                        "Test address",
                        Set.of(new OrderItemRequestDto(1L, 2)));

        assertThat(validator.validate(request))
                .isEmpty();
    }

    // OrderPaymentRequestDto

    @Test
    void orderPayment_shouldRejectNullPaymentMethod() {
        OrderPaymentRequestDto request =
                new OrderPaymentRequestDto(null);

        assertThat(validator.validate(request))
                .anyMatch(error ->
                        error.getPropertyPath()
                                .toString()
                                .equals("paymentMethod")
                );
    }

    @Test
    void orderPayment_shouldAcceptValidRequest() {
        OrderPaymentRequestDto request =
                new OrderPaymentRequestDto(PaymentMethod.CARD);

        assertThat(validator.validate(request))
                .isEmpty();
    }
}
