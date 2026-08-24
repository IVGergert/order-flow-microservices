package com.gergert.orderservice.service;

import com.gergert.common.dto.OrderPaymentRequestDto;
import com.gergert.orderservice.dto.CreateOrderRequestDto;
import com.gergert.orderservice.entity.Order;

import java.util.List;

public interface OrderService {
    Order processPayment(Long id, OrderPaymentRequestDto requestDto, Long customerId);
    Order create(CreateOrderRequestDto request, Long customerId);
    Order getOrderOrThrow(Long id);
    List<Order> getAllOrdersByUserId(Long customerId);

}
