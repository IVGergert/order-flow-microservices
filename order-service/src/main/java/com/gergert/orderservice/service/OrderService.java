package com.gergert.orderservice.service;

import com.gergert.common.dto.OrderPaymentRequestDto;
import com.gergert.common.dto.CreatePaymentRequestDto;
import com.gergert.common.enums.PaymentStatus;
import com.gergert.orderservice.client.PaymentHttpClient;
import com.gergert.orderservice.dto.CreateOrderRequestDto;
import com.gergert.orderservice.dto.OrderMapper;
import com.gergert.common.dto.kafka.OrderPaidEventDto;
import com.gergert.orderservice.entity.Order;
import com.gergert.orderservice.entity.OrderItem;
import com.gergert.orderservice.entity.OrderStatus;
import com.gergert.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@RequiredArgsConstructor
@Service
public class OrderService {
    @Value("${kafka.topics.order-events}")
    private String orderEventsTopic;

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final PaymentHttpClient paymentHttpClient;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public Order create(CreateOrderRequestDto request) {
        var entity = orderMapper.toEntity(request);
        calculatePricingForOrder(entity);
        entity.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        return orderRepository.save(entity);
    }

    public Order getOrderOrThrow(Long id) {
        var orderItemOptional = orderRepository.findById(id);
        return orderItemOptional.orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Entity with id `%s` not found".formatted(id)));
    }

    private void calculatePricingForOrder(Order order){
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (OrderItem item : order.getItems()) {
            var randomPrice = ThreadLocalRandom.current().nextDouble(100, 4000);
            item.setPriceAtPurchase(BigDecimal.valueOf(randomPrice));

            totalPrice = item.getPriceAtPurchase()
                    .multiply(BigDecimal.valueOf(item.getQuantity()))
                    .add(totalPrice);
        }

        order.setTotalAmount(totalPrice);
    }

    public Order processPayment(Long id,  OrderPaymentRequestDto requestDto){
        var entity = getOrderOrThrow(id);

        if (!entity.getOrderStatus().equals(OrderStatus.PENDING_PAYMENT)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order must be in orderStatus PENDING_PAYMENT");
        }

        var response = paymentHttpClient.createPayment(CreatePaymentRequestDto.builder()
                        .orderId(id)
                        .paymentMethod(requestDto.paymentMethod())
                        .amount(entity.getTotalAmount())
                .build());

        var status = PaymentStatus.PAYMENT_SUCCEEDED.equals(response.paymentStatus())
                ? OrderStatus.PAID
                : OrderStatus.PAYMENT_FAILED;

        entity.setOrderStatus(status);
        Order savedOrder = orderRepository.save(entity);

        if (status == OrderStatus.PAID){
            OrderPaidEventDto event = OrderPaidEventDto.builder()
                    .orderId(savedOrder.getId())
                    .address(savedOrder.getAddress())
                    .amount(savedOrder.getTotalAmount())
                    .build();

            log.info("Sending OrderPaidEvent for orderId={}", savedOrder.getId());

            kafkaTemplate.send(
                    orderEventsTopic,
                    savedOrder.getId().toString(),
                    event
            ).thenAccept(result ->
                    log.info("OrderPaidEvent sent for orderId={}", savedOrder.getId())
            );
        }

        return savedOrder;
    }
}
