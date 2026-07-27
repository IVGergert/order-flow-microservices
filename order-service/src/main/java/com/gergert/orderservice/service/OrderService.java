package com.gergert.orderservice.service;

import com.gergert.common.dto.OrderPaymentRequestDto;
import com.gergert.orderservice.dto.CreateOrderRequestDto;
import com.gergert.orderservice.dto.OrderDto;
import com.gergert.orderservice.entity.Order;

import java.util.List;

public interface OrderService {
    Order processPayment(Long id, OrderPaymentRequestDto requestDto);
    Order create(CreateOrderRequestDto request);
    Order getOrderOrThrow(Long id);
}
