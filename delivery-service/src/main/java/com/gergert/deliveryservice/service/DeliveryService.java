package com.gergert.deliveryservice.service;

import com.gergert.common.dto.kafka.OrderPaidEventDto;

public interface DeliveryService {
    void createDelivery(OrderPaidEventDto eventDto);
    void pickUpOrder(Long orderId);
    void completeDelivery(Long orderId);
}
