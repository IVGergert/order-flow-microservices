package com.gergert.deliveryservice.dto;

import com.gergert.deliveryservice.entity.CourierStatus;

public record CourierStatusResponseDto (
        CourierStatus status
){}
