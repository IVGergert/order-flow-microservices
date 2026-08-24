package com.gergert.deliveryservice.dto;

import com.gergert.deliveryservice.entity.DeliveryStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record DeliveryResponseDto(
        Long id,
        Long orderId,
        DeliveryStatus deliveryStatus,
        Long courierId,
        String courierName,
        String address,
        Integer etaMinutes,
        LocalDateTime completedAt) {
}
