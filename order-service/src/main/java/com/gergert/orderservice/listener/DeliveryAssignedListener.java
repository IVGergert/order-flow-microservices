package com.gergert.orderservice.listener;

import com.gergert.common.dto.kafka.DeliveryAssignedEventDto;
import com.gergert.orderservice.entity.Order;
import com.gergert.orderservice.entity.OrderStatus;
import com.gergert.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryAssignedListener {
    private final OrderRepository orderRepository;

    @KafkaListener(
            topics = "${kafka.topics.delivery-events}",
            groupId = "order-group"
    )

    public void handle(DeliveryAssignedEventDto eventDto) {

        Order order = orderRepository.findById(eventDto.orderId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Entity with id `%s` not found".formatted(eventDto.orderId())
                ));

        if (order.getOrderStatus() == OrderStatus.DELIVERY_ASSIGNED) {
            log.info("Order {} already in DELIVERY_ASSIGNED status, skipping", eventDto.orderId());
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
