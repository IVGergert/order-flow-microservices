package com.gergert.orderservice.service;

import com.gergert.orderservice.client.PaymentHttpClient;
import com.gergert.orderservice.dto.CreateOrderRequestDto;
import com.gergert.orderservice.dto.OrderMapper;
import com.gergert.orderservice.dto.payment.CreatePaymentRequestDto;
import com.gergert.orderservice.dto.payment.OrderPaymentRequestDto;
import com.gergert.orderservice.entity.Order;
import com.gergert.orderservice.entity.OrderItem;
import com.gergert.orderservice.entity.OrderStatus;
import com.gergert.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

@RequiredArgsConstructor
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final PaymentHttpClient paymentHttpClient;

    public Order create(CreateOrderRequestDto request) {
        var entity = orderMapper.toEntity(request);
        calculatePricingForOrder(entity);
        entity.setStatus(OrderStatus.PENDING_PAYMENT);
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

        if (!entity.getStatus().equals(OrderStatus.PENDING_PAYMENT)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order must be in status PENDING_PAYMENT");
        }

        var response = paymentHttpClient.createPayment(CreatePaymentRequestDto.builder()
                        .orderId(id)
                        .paymentMethod(requestDto.paymentMethod())
                        .amount(entity.getTotalAmount())
                .build());


        var status = response.paymentStatus().equals("PAYMENT_SUCCEEDED")
                ? OrderStatus.PAID
                : OrderStatus.PAYMENT_FAILED;

        entity.setStatus(status);
        return orderRepository.save(entity);
    }
}
