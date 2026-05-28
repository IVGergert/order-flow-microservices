package com.gergert.orderservice.controller;

import com.gergert.orderservice.dto.CreateOrderRequestDto;
import com.gergert.orderservice.dto.OrderDto;
import com.gergert.orderservice.dto.OrderMapper;
import com.gergert.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;

    @PostMapping
    public OrderDto create(@RequestBody CreateOrderRequestDto request) {
        log.info("Creating order: {}", request);
        var saved = orderService.create(request);
        return orderMapper.toOrderDto(saved);
    }

    @GetMapping("/{id}")
    public OrderDto getOne(@PathVariable Long id) {
        log.info("Retrieving order with id {}", id);
        var found = orderService.getOrderOrThrow(id);
        return orderMapper.toOrderDto(found);
    }
}
