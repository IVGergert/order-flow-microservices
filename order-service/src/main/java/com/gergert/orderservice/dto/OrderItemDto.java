package com.gergert.orderservice.dto;

import java.math.BigDecimal;

public record OrderItemDto(
        Long id,
        Long itemId,
        String itemName,
        BigDecimal priceAtPurchase,
        Integer quantity)
{}