package com.gergert.orderservice.dto;

public record OrderItemRequestDto (
        Long itemId,
        Integer quantity,
        String itemName)
{}
