package com.gergert.orderservice.service.impl;

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
import com.gergert.orderservice.service.OrderService;
import jakarta.transaction.Transactional;
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
public class OrderServiceImpl implements OrderService {
    @Value("${kafka.topics.order-paid-events}")
    private String orderPaidEventTopic;

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final PaymentHttpClient paymentHttpClient;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    @Override
    public Order processPayment(Long id, OrderPaymentRequestDto requestDto, Long customerId){
        var order = getOrderOrThrow(id);

        if (!order.getCustomerId().equals(customerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only pay for your own orders");
        }

        if (!order.getOrderStatus().equals(OrderStatus.PENDING_PAYMENT)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order must be in orderStatus PENDING_PAYMENT");
        }

        var response = paymentHttpClient.createPayment(CreatePaymentRequestDto.builder()
                        .orderId(id)
                        .paymentMethod(requestDto.paymentMethod())
                        .amount(order.getTotalAmount())
                .build());

        var status = PaymentStatus.PAYMENT_SUCCEEDED.equals(response.paymentStatus())
                ? OrderStatus.PAID
                : OrderStatus.PAYMENT_FAILED;

        order.setOrderStatus(status);
        Order savedOrder = orderRepository.save(order);

        if (status == OrderStatus.PAID){
            OrderPaidEventDto event = OrderPaidEventDto.builder()
                    .orderId(savedOrder.getId())
                    .address(savedOrder.getAddress())
                    .amount(savedOrder.getTotalAmount())
                    .build();

            log.info("Sending OrderPaidEvent to Kafka for orderId={}", savedOrder.getId());

            kafkaTemplate.send(
                    orderPaidEventTopic,
                    savedOrder.getId().toString(),
                    event
            );
        }

        return savedOrder;
    }

    @Override
    public Order create(CreateOrderRequestDto request, Long customerId) {
        var order = orderMapper.toEntity(request);

        order.setCustomerId(customerId);

        calculatePricingForOrder(order);
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT);

        Order savedOrder = orderRepository.save(order);
        log.info("Order was created with id={}", savedOrder.getId());

        return savedOrder;
    }

    @Override
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
}
