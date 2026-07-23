package com.gergert.common.dto.kafka;

import lombok.Builder;

@Builder
public record DeliveryAssignedEventDto(
        Long orderId,
        Long courierId,
        String courierName,
        String address,
        Integer etaMinutes
)
{}
