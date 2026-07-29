package com.gergert.deliveryservice.service;

import com.gergert.common.dto.kafka.OrderPaidEventDto;
import com.gergert.deliveryservice.dto.CourierStatisticsResponseDto;
import com.gergert.deliveryservice.dto.DeliveryResponseDto;

import java.util.List;
import java.util.Optional;

public interface DeliveryService {
    void createDelivery(OrderPaidEventDto eventDto);
    DeliveryResponseDto pickUpOrder(Long orderId, Long courierUserId);
    DeliveryResponseDto completeDelivery(Long orderId, Long courierUserId);
    Optional<DeliveryResponseDto> getCurrentDeliveryByCourierUserId(Long courierUserId);
    DeliveryResponseDto acceptDelivery(Long orderId, Long courierUserId);
    List<DeliveryResponseDto> getDeliveriesByCourierUserId(Long courierUserId);
    DeliveryResponseDto getDeliveryByOrderId(Long orderId, Long courierUserId);
    List<DeliveryResponseDto> getWaitingDeliveries(Long courierUserId);

    CourierStatisticsResponseDto getCompletedDeliveriesToday(Long courierUserId);
}
