package com.gergert.orderservice.service.impl;

import com.gergert.common.dto.OrderPaymentRequestDto;
import com.gergert.common.dto.CreatePaymentRequestDto;
import com.gergert.common.enums.PaymentMethod;
import com.gergert.common.enums.PaymentStatus;
import com.gergert.orderservice.client.PaymentHttpClient;
import com.gergert.orderservice.dto.CreateOrderRequestDto;
import com.gergert.orderservice.dto.OrderMapper;
import com.gergert.common.dto.kafka.OrderPaidEventDto;
import com.gergert.orderservice.entity.MenuItem;
import com.gergert.orderservice.entity.Order;
import com.gergert.orderservice.entity.OrderItem;
import com.gergert.orderservice.entity.OrderStatus;
import com.gergert.orderservice.exception.InvalidOrderStatusException;
import com.gergert.orderservice.exception.MenuItemNotFoundException;
import com.gergert.orderservice.exception.OrderAccessDeniedException;
import com.gergert.orderservice.exception.OrderNotFoundException;
import com.gergert.orderservice.repository.MenuItemRepository;
import com.gergert.orderservice.repository.OrderRepository;
import com.gergert.orderservice.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class OrderServiceImpl implements OrderService {
    @Value("${kafka.topics.order-paid-events}")
    private String orderPaidEventTopic;

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;

    private final OrderMapper orderMapper;
    private final PaymentHttpClient paymentHttpClient;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    @Override
    public Order processPayment(Long id, OrderPaymentRequestDto requestDto, Long customerId){
        var order = getOrderOrThrow(id);

        if (!order.getCustomerId().equals(customerId)) {
            throw new OrderAccessDeniedException("You can only pay for your own orders");
        }

        if (!order.getOrderStatus().equals(OrderStatus.PENDING_PAYMENT)){
            throw new InvalidOrderStatusException("Order must be in orderStatus PENDING_PAYMENT");

        }

        if (requestDto.paymentMethod() == PaymentMethod.CASH) {
            order.setOrderStatus(OrderStatus.CASH_ON_DELIVERY);
            Order savedOrder = orderRepository.save(order);
            sendOrderReadyForDeliveryEvent(savedOrder);
            return savedOrder;
        }

        var response = paymentHttpClient.createPayment(
                CreatePaymentRequestDto.builder()
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
            sendOrderReadyForDeliveryEvent(savedOrder);
        }

        return savedOrder;
    }

    @Transactional
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
    @Transactional(readOnly = true)
    public Order getOrderOrThrow(Long id) {
        var orderItemOptional = orderRepository.findById(id);
        return orderItemOptional.orElseThrow(() ->
                new OrderNotFoundException("Entity with id `%s` not found".formatted(id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getAllOrdersByUserId(Long customerId) {
        return orderRepository.findAllByCustomerId(customerId);
    }

    private void sendOrderReadyForDeliveryEvent(Order order) {

        OrderPaidEventDto event =
                OrderPaidEventDto.builder()
                        .orderId(order.getId())
                        .address(order.getAddress())
                        .amount(order.getTotalAmount())
                        .build();

        log.info("Sending OrderReadyForDeliveryEvent for orderId={}", order.getId());

        kafkaTemplate.send(
                orderPaidEventTopic,
                order.getId().toString(),
                event
        );
    }

    private void calculatePricingForOrder(Order order){
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (OrderItem orderItem : order.getItems()) {
            MenuItem menuItem = menuItemRepository
                    .findById(orderItem.getItemId())
                    .orElseThrow(() -> new MenuItemNotFoundException(
                            "Menu item with id `%s` not found".formatted(orderItem.getItemId())
                    )
            );

            orderItem.setItemName(menuItem.getName());
            orderItem.setPriceAtPurchase(menuItem.getPrice());
            orderItem.setOrder(order);

            BigDecimal itemTotal = menuItem
                    .getPrice()
                    .multiply(BigDecimal.valueOf(orderItem.getQuantity()
                    )
            );

            totalPrice = totalPrice.add(itemTotal);
        }

        order.setTotalAmount(totalPrice);
    }
}
