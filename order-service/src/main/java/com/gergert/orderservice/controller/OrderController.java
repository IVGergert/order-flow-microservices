package com.gergert.orderservice.controller;

import com.gergert.orderservice.dto.CreateOrderRequestDto;
import com.gergert.orderservice.dto.OrderDto;
import com.gergert.orderservice.dto.OrderMapper;
import com.gergert.common.dto.OrderPaymentRequestDto;
import com.gergert.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OrderController {
    private final OrderService orderService;
    private final OrderMapper orderMapper;

    @PostMapping
    public OrderDto create(@RequestBody CreateOrderRequestDto requestDto) {
        log.info("Creating order: {}", requestDto);
        var saved = orderService.create(requestDto);
        return orderMapper.toOrderDto(saved);
    }

    @GetMapping("/{id}")
    public OrderDto getOne(@PathVariable Long id) {
        log.info("Retrieving order with id {}", id);
        var found = orderService.getOrderOrThrow(id);
        return orderMapper.toOrderDto(found);
    }

    @PostMapping("/{id}/pay")
    public OrderDto payOrder(@PathVariable Long id,
                             @RequestBody OrderPaymentRequestDto requestDto) {
        log.info("Paying order with id={}, request={}", id, requestDto);
        var entity = orderService.processPayment(id, requestDto);
        return orderMapper.toOrderDto(entity);
    }
}
