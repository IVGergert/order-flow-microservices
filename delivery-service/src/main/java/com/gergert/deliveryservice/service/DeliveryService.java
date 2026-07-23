package com.gergert.deliveryservice.service;

import com.gergert.common.dto.kafka.OrderPaidEventDto;

public interface DeliveryService {
    void createDelivery(OrderPaidEventDto eventDto);
}
