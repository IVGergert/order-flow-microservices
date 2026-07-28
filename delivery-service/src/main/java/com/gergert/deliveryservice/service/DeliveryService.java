package com.gergert.deliveryservice.service;

import com.gergert.common.dto.kafka.OrderPaidEventDto;
import com.gergert.deliveryservice.dto.DeliveryResponseDto;

import java.util.List;

public interface DeliveryService {
    void createDelivery(OrderPaidEventDto eventDto);
    void pickUpOrder(Long orderId, Long courierUserId);
    void completeDelivery(Long orderId, Long courierUserId);
    List<DeliveryResponseDto> getDeliveriesByCourierUserId(Long courierUserId);
}
