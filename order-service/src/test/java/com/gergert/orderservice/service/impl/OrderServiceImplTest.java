package com.gergert.orderservice.service.impl;

import com.gergert.common.dto.CreatePaymentResponseDto;
import com.gergert.common.dto.OrderPaymentRequestDto;
import com.gergert.common.enums.PaymentMethod;
import com.gergert.common.enums.PaymentStatus;
import com.gergert.orderservice.client.PaymentHttpClient;
import com.gergert.orderservice.dto.CreateOrderRequestDto;
import com.gergert.orderservice.dto.OrderMapper;
import com.gergert.orderservice.dto.OrderItemRequestDto;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private MenuItemRepository menuItemRepository;
    @Mock private OrderMapper orderMapper;
    @Mock private PaymentHttpClient paymentHttpClient;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @InjectMocks private OrderServiceImpl orderService;

    private Order order;
    private MenuItem pizza;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderService, "orderPaidEventTopic", "order-paid");

        pizza = new MenuItem();
        pizza.setId(10L);
        pizza.setName("Pizza");
        pizza.setPrice(new BigDecimal("12.50"));

        OrderItem item = new OrderItem();
        item.setItemId(10L);
        item.setQuantity(2);

        order = new Order();
        order.setId(1L);
        order.setCustomerId(100L);
        order.setAddress("Test address");
        order.setItems(new LinkedHashSet<>(List.of(item)));
    }

    @Test
    void create_shouldCalculatePricingAndSetPendingPayment() {
        var request = new CreateOrderRequestDto("Test address", new LinkedHashSet<>());
        when(orderMapper.toEntity(request)).thenReturn(order);
        when(menuItemRepository.findById(10L)).thenReturn(Optional.of(pizza));
        when(orderRepository.save(order)).thenReturn(order);

        Order result = orderService.create(request, 100L);

        assertThat(result.getCustomerId()).isEqualTo(100L);
        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(result.getTotalAmount()).isEqualByComparingTo("25.00");
        assertThat(order.getItems().iterator().next().getItemName()).isEqualTo("Pizza");
        assertThat(order.getItems().iterator().next().getPriceAtPurchase()).isEqualByComparingTo("12.50");
        verify(orderRepository).save(order);
    }

    @Test
    void create_shouldFailWhenMenuItemDoesNotExist() {
        var request = new CreateOrderRequestDto("Test address", new LinkedHashSet<>());
        when(orderMapper.toEntity(request)).thenReturn(order);
        when(menuItemRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.create(request, 100L))
                .isInstanceOf(MenuItemNotFoundException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void getOrderOrThrow_shouldReturnOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThat(orderService.getOrderOrThrow(1L)).isSameAs(order);
        verify(orderRepository).findById(1L);
    }

    @Test
    void getOrderOrThrow_shouldReturnNotFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderOrThrow(1L))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void getAllOrdersByUserId_shouldDelegateToRepository() {
        when(orderRepository.findAllByCustomerId(100L)).thenReturn(List.of(order));

        assertThat(orderService.getAllOrdersByUserId(100L)).containsExactly(order);
        verify(orderRepository).findAllByCustomerId(100L);
    }

    @Test
    void processPayment_cash_shouldMarkOrderAsCashOnDeliveryAndPublishEvent() {
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        Order result = orderService.processPayment(1L, new OrderPaymentRequestDto(PaymentMethod.CASH), 100L);

        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.CASH_ON_DELIVERY);
        verify(orderRepository).save(order);
        verify(kafkaTemplate).send(anyString(), eq("1"), any());
        verifyNoInteractions(paymentHttpClient);
    }

    @Test
    void processPayment_card_success_shouldMarkPaidAndPublishEvent() {
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        order.setTotalAmount(new BigDecimal("25.00"));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentHttpClient.createPayment(any())).thenReturn(
                new CreatePaymentResponseDto(5L, 1L, new BigDecimal("25.00"),
                        PaymentStatus.PAYMENT_SUCCEEDED, PaymentMethod.CARD));
        when(orderRepository.save(order)).thenReturn(order);

        Order result = orderService.processPayment(1L, new OrderPaymentRequestDto(PaymentMethod.CARD), 100L);

        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        verify(paymentHttpClient).createPayment(argThat(request ->
                request.orderId().equals(1L)
                        && request.amount().compareTo(new BigDecimal("25.00")) == 0
                        && request.paymentMethod() == PaymentMethod.CARD));
        verify(kafkaTemplate).send(anyString(), eq("1"), any());
    }

    @Test
    void processPayment_card_failure_shouldMarkPaymentFailedWithoutPublishingEvent() {
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentHttpClient.createPayment(any())).thenReturn(
                new CreatePaymentResponseDto(5L, 1L, new BigDecimal("25.00"),
                        PaymentStatus.PAYMENT_FAILED, PaymentMethod.CARD));
        when(orderRepository.save(order)).thenReturn(order);

        Order result = orderService.processPayment(1L, new OrderPaymentRequestDto(PaymentMethod.CARD), 100L);

        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void processPayment_shouldRejectAnotherCustomersOrder() {
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.processPayment(1L,
                new OrderPaymentRequestDto(PaymentMethod.CARD), 999L))
                .isInstanceOf(OrderAccessDeniedException.class);

        verifyNoInteractions(paymentHttpClient);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void processPayment_shouldRejectOrderWithWrongStatus() {
        order.setOrderStatus(OrderStatus.PAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.processPayment(1L,
                new OrderPaymentRequestDto(PaymentMethod.CARD), 100L))
                .isInstanceOf(InvalidOrderStatusException.class);

        verifyNoInteractions(paymentHttpClient);
        verify(orderRepository, never()).save(any());
    }
}
