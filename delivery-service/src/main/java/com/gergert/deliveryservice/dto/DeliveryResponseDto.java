package com.gergert.deliveryservice.dto;

import com.gergert.deliveryservice.entity.DeliveryStatus;
import com.gergert.deliveryservice.entity.TransportType;
import lombok.Builder;

@Builder
public record DeliveryResponseDto(
        Long id,
        Long orderId,
        DeliveryStatus deliveryStatus,
        Long courierId,
        String courierName,
        String address,
        Integer etaMinutes
) {
}
