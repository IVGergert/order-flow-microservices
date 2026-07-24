package com.gergert.orderservice.kafka;


import com.gergert.common.dto.kafka.OrderPickedUpEventDto;
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
public class OrderPickedUpListener {
    private final OrderRepository orderRepository;

    @Transactional
    @KafkaListener(
            topics = "${kafka.topics.order-picked-up-events}",
            groupId = "order-group"
    )
    public void handle(OrderPickedUpEventDto eventDto) {
        log.info("Received OrderPickedUpEvent for orderId={}", eventDto.orderId());

        Order order = orderRepository.findById(eventDto.orderId())
                .orElseThrow(() -> new IllegalArgumentException("Order with id %d not found".formatted(eventDto.orderId())));

        if (order.getOrderStatus() == OrderStatus.IN_DELIVERY) {
            return;
        }

        order.setOrderStatus(OrderStatus.IN_DELIVERY);
        orderRepository.save(order);

        log.info("Order {} status updated to IN_DELIVERY", eventDto.orderId());
    }
}
