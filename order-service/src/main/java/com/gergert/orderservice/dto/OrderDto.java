package com.gergert.orderservice.dto;

import com.gergert.orderservice.entity.OrderStatus;

import java.math.BigDecimal;
import java.util.Set;

public record OrderDto(
        Long id,
        Long customerId,
        String address,
        BigDecimal totalAmount,
        OrderStatus orderStatus,
        String courierName,
        Integer etaMinutes,
        Set<OrderItemDto> items)
{}