package com.gergert.orderservice.dto;

import java.util.Set;

public record CreateOrderRequestDto(
        String address,
        Set<OrderItemRequestDto> items)
{}
