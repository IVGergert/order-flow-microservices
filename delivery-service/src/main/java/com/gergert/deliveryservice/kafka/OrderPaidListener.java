package com.gergert.deliveryservice.kafka;

import com.gergert.common.dto.kafka.OrderPaidEventDto;
import com.gergert.deliveryservice.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPaidListener {
    private final DeliveryService deliveryService;

    @KafkaListener(
            topics = "${kafka.topics.order-paid-events}",
            groupId = "delivery-group"
    )
    public void handle(OrderPaidEventDto eventDto){
        log.info("Received OrderPaidEvent: orderId={}", eventDto.orderId());
        deliveryService.createDelivery(eventDto);
    }
}
