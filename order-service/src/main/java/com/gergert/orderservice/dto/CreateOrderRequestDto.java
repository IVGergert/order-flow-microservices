package com.gergert.orderservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateOrderRequestDto(
        @NotBlank(message = "Address cannot be empty")
        @Size(max = 255, message = "Address must not exceed 255 characters")
        String address,

        @NotEmpty(message = "Order must contain at least one item")
        Set<@Valid OrderItemRequestDto> items)
{}
