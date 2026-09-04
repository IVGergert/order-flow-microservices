package com.gergert.orderservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderItemRequestDto (
        @NotNull(message = "Item ID must not be null")
        @Positive(message = "Item ID must be positive")
        Long itemId,

        @NotNull(message = "Quantity must not be null")
        @Positive(message = "Quantity must be positive")
        Integer quantity
)
{}
