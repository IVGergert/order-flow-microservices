package com.gergert.orderservice.kafka;

import com.gergert.common.dto.kafka.OrderDeliveredEventDto;
import com.gergert.orderservice.entity.Order;
import com.gergert.orderservice.entity.OrderStatus;
import com.gergert.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderDeliveredListener {
    private final OrderRepository orderRepository;

    @Transactional
    @KafkaListener(
            topics = "${kafka.topics.delivery-completed-events}",
            groupId = "order-group"
    )
    public void handle(OrderDeliveredEventDto eventDto) {
        log.info("Received OrderDeliveredEvent for orderId={}", eventDto.orderId());

        Order order = orderRepository.findById(eventDto.orderId())
                .orElseThrow(() -> new IllegalArgumentException("Order with id %d not found".formatted(eventDto.orderId())));

        if (order.getOrderStatus() == OrderStatus.DELIVERED) {
            return;
        }

        // МЕНЯЕМ СТАТУС В ORDER SERVICE!
        order.setOrderStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);

        log.info("Order {} status updated to DELIVERED 🎉", eventDto.orderId());
    }
}
