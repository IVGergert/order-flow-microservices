package com.gergert.orderservice.kafka;

import com.gergert.common.dto.kafka.DeliveryAssignedEventDto;
import com.gergert.orderservice.entity.Order;
import com.gergert.orderservice.entity.OrderStatus;
import com.gergert.orderservice.repository.OrderRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryAssignedListener {
    private final OrderRepository orderRepository;

    @Transactional
    @KafkaListener(
            topics = "${kafka.topics.delivery-assigned-events}",
            groupId = "order-group"
    )

    public void handle(DeliveryAssignedEventDto eventDto) {
        log.info("Received DeliveryAssignedEvent: orderId={}", eventDto.orderId());

        Order order = orderRepository.findById(eventDto.orderId())
                .orElseThrow(() -> new IllegalArgumentException(("Order with id %d not " +
                        "found").formatted(eventDto.orderId())));

        if (order.getOrderStatus() == OrderStatus.DELIVERY_ASSIGNED) {
            log.info("Order {} already in DELIVERY_ASSIGNED status", eventDto.orderId());
            return;
        }

        log.info("Received DeliveryAssignedEvent: orderId={}", eventDto.orderId());

        order.setOrderStatus(OrderStatus.DELIVERY_ASSIGNED);
        order.setCourierName(eventDto.courierName());
        order.setEtaMinutes(eventDto.etaMinutes());
        orderRepository.save(order);

        log.info("Order {} updated to DELIVERY_ASSIGNED, courier={}, eta={}min",
                eventDto.orderId(),
                eventDto.courierName(),
                eventDto.etaMinutes());
    }
}
