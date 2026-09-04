package com.gergert.orderservice.controller;

import com.gergert.common.dto.jwt.JwtClaimsDto;
import com.gergert.orderservice.dto.CreateOrderRequestDto;
import com.gergert.orderservice.dto.OrderDto;
import com.gergert.orderservice.dto.OrderMapper;
import com.gergert.common.dto.OrderPaymentRequestDto;
import com.gergert.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    private final OrderMapper orderMapper;

    @PostMapping
    public OrderDto create(@Valid @RequestBody CreateOrderRequestDto requestDto,
                           @AuthenticationPrincipal JwtClaimsDto claims) {

        log.info("Create order request by user userId={}, email={}", claims.userId(), claims.email());
        var saved = orderService.create(requestDto, claims.userId());
        return orderMapper.toOrderDto(saved);
    }

    @PostMapping("/{id}/pay")
    public OrderDto payOrder(@PathVariable Long id,
                             @Valid @RequestBody OrderPaymentRequestDto requestDto,
                             @AuthenticationPrincipal JwtClaimsDto claims) {

        log.info("Paying order with id={}, request={}", id, requestDto);
        var entity = orderService.processPayment(id, requestDto, claims.userId());
        return orderMapper.toOrderDto(entity);
    }

    @GetMapping("/my")
    public List<OrderDto> getMyOrder(@AuthenticationPrincipal JwtClaimsDto claims) {
        log.info("View orders for user with id = {}", claims.userId());
        var orders = orderService.getAllOrdersByUserId(claims.userId());
        return orderMapper.toOrderDto(orders);
    }
}
