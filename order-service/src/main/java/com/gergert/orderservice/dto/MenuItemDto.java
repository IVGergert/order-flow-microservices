package com.gergert.orderservice.dto;

import com.gergert.orderservice.entity.MenuCategory;

import java.math.BigDecimal;

public record MenuItemDto(
        Long id,
        String name,
        BigDecimal price,
        String description,
        MenuCategory category,
        String categoryTitle,
        String imageUrl) {
}
