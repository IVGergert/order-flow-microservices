package com.gergert.orderservice.dto.kafka;

import lombok.Builder;

@Builder
public record OrderPaidEvent(
        Long orderId,
        String address)
{}
