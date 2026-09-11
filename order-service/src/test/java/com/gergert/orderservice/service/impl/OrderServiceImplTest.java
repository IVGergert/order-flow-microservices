package com.gergert.orderservice.service.impl;

import com.gergert.common.dto.CreatePaymentRequestDto;
import com.gergert.common.dto.CreatePaymentResponseDto;
import com.gergert.common.dto.OrderPaymentRequestDto;
import com.gergert.common.dto.kafka.OrderPaidEventDto;
import com.gergert.common.enums.PaymentMethod;
import com.gergert.common.enums.PaymentStatus;
import com.gergert.orderservice.client.PaymentHttpClient;
import com.gergert.orderservice.dto.CreateOrderRequestDto;
import com.gergert.orderservice.dto.OrderMapper;
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

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private PaymentHttpClient paymentHttpClient;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order order;
    private OrderItem orderItem;
    private MenuItem pizza;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                orderService,
                "orderPaidEventTopic",
                "order-paid"
        );

        pizza = new MenuItem();
        pizza.setId(10L);
        pizza.setName("Pizza");
        pizza.setPrice(new BigDecimal("12.50"));

        orderItem = new OrderItem();
        orderItem.setItemId(10L);
        orderItem.setQuantity(2);

        order = new Order();
        order.setId(1L);
        order.setCustomerId(100L);
        order.setAddress("Test address");
        order.setItems(new LinkedHashSet<>(List.of(orderItem)));
    }

    // create()

    @Test
    void create_shouldCalculatePricingAndSetPendingPayment() {
        CreateOrderRequestDto request = new CreateOrderRequestDto(
                        "Test address",
                        new LinkedHashSet<>());

        when(orderMapper.toEntity(request))
                .thenReturn(order);

        when(menuItemRepository.findById(10L))
                .thenReturn(Optional.of(pizza));

        when(orderRepository.save(order))
                .thenReturn(order);

        Order result =
                orderService.create(request, 100L);

        assertThat(result)
                .isSameAs(order);

        assertThat(result.getCustomerId())
                .isEqualTo(100L);

        assertThat(result.getOrderStatus())
                .isEqualTo(OrderStatus.PENDING_PAYMENT);

        assertThat(result.getTotalAmount())
                .isEqualByComparingTo("25.00");

        assertThat(orderItem.getItemName())
                .isEqualTo("Pizza");

        assertThat(orderItem.getPriceAtPurchase())
                .isEqualByComparingTo("12.50");

        assertThat(orderItem.getOrder())
                .isSameAs(order);

        verify(orderMapper)
                .toEntity(request);

        verify(menuItemRepository)
                .findById(10L);

        verify(orderRepository)
                .save(order);

        verifyNoInteractions(paymentHttpClient);
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void create_shouldCalculateTotalPriceForMultipleItems() {
        MenuItem burger = new MenuItem();
        burger.setId(20L);
        burger.setName("Burger");
        burger.setPrice(new BigDecimal("8.00"));

        OrderItem burgerItem = new OrderItem();
        burgerItem.setItemId(20L);
        burgerItem.setQuantity(3);

        order.getItems().add(burgerItem);

        CreateOrderRequestDto request = new CreateOrderRequestDto(
                        "Test address",
                        new LinkedHashSet<>());

        when(orderMapper.toEntity(request))
                .thenReturn(order);

        when(menuItemRepository.findById(10L))
                .thenReturn(Optional.of(pizza));

        when(menuItemRepository.findById(20L))
                .thenReturn(Optional.of(burger));

        when(orderRepository.save(order))
                .thenReturn(order);

        Order result =
                orderService.create(request, 100L);

        assertThat(result.getTotalAmount())
                .isEqualByComparingTo("49.00");

        assertThat(orderItem.getPriceAtPurchase())
                .isEqualByComparingTo("12.50");

        assertThat(burgerItem.getPriceAtPurchase())
                .isEqualByComparingTo("8.00");

        assertThat(burgerItem.getItemName())
                .isEqualTo("Burger");

        assertThat(burgerItem.getOrder())
                .isSameAs(order);

        verify(menuItemRepository)
                .findById(10L);

        verify(menuItemRepository)
                .findById(20L);

        verify(orderRepository)
                .save(order);
    }

    @Test
    void create_shouldThrowWhenMenuItemDoesNotExist() {
        CreateOrderRequestDto request = new CreateOrderRequestDto(
                        "Test address",
                        new LinkedHashSet<>()
                );

        when(orderMapper.toEntity(request))
                .thenReturn(order);

        when(menuItemRepository.findById(10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.create(request, 100L))
                .isInstanceOf(MenuItemNotFoundException.class)
                .hasMessage("Menu item with id `10` not found");

        verify(orderMapper)
                .toEntity(request);

        verify(menuItemRepository)
                .findById(10L);

        verify(orderRepository, never())
                .save(any(Order.class));

        verifyNoInteractions(paymentHttpClient);
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void create_shouldStopPricingWhenOneOfMenuItemsDoesNotExist() {
        MenuItem burger = new MenuItem();
        burger.setId(20L);
        burger.setName("Burger");
        burger.setPrice(new BigDecimal("8.00"));

        OrderItem burgerItem = new OrderItem();
        burgerItem.setItemId(20L);
        burgerItem.setQuantity(1);

        order.getItems().add(burgerItem);

        CreateOrderRequestDto request = new CreateOrderRequestDto(
                        "Test address",
                        new LinkedHashSet<>()
                );

        when(orderMapper.toEntity(request))
                .thenReturn(order);

        when(menuItemRepository.findById(10L))
                .thenReturn(Optional.of(pizza));

        when(menuItemRepository.findById(20L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.create(request, 100L))
                .isInstanceOf(MenuItemNotFoundException.class)
                .hasMessage("Menu item with id `20` not found");

        verify(menuItemRepository)
                .findById(10L);

        verify(menuItemRepository)
                .findById(20L);

        verify(orderRepository, never())
                .save(any(Order.class));
    }

    // processPayment()

    @Test
    void processPayment_cash_shouldMarkOrderAsCashOnDelivery() {
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        order.setTotalAmount(new BigDecimal("25.00"));

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(order))
                .thenReturn(order);

        Order result = orderService.processPayment(
                        1L,
                        new OrderPaymentRequestDto(PaymentMethod.CASH),
                        100L);

        assertThat(result.getOrderStatus())
                .isEqualTo(OrderStatus.CASH_ON_DELIVERY);

        verify(orderRepository)
                .findById(1L);

        verify(orderRepository)
                .save(order);

        verifyNoInteractions(paymentHttpClient);

        ArgumentCaptor<OrderPaidEventDto> eventCaptor =
                ArgumentCaptor.forClass(OrderPaidEventDto.class);

        verify(kafkaTemplate).send(
                        eq("order-paid"),
                        eq("1"),
                        eventCaptor.capture()
        );

        OrderPaidEventDto event =
                eventCaptor.getValue();

        assertThat(event.orderId())
                .isEqualTo(1L);

        assertThat(event.address())
                .isEqualTo("Test address");

        assertThat(event.amount())
                .isEqualByComparingTo("25.00");
    }

    @Test
    void processPayment_card_success_shouldMarkOrderAsPaidAndPublishEvent() {
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        order.setTotalAmount(new BigDecimal("25.00"));

        CreatePaymentResponseDto paymentResponse = new CreatePaymentResponseDto(
                        5L,
                        1L,
                        new BigDecimal("25.00"),
                        PaymentStatus.PAYMENT_SUCCEEDED,
                        PaymentMethod.CARD);

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(paymentHttpClient.createPayment(any(CreatePaymentRequestDto.class)))
                .thenReturn(paymentResponse);

        when(orderRepository.save(order))
                .thenReturn(order);

        Order result = orderService.processPayment(
                        1L,
                        new OrderPaymentRequestDto(PaymentMethod.CARD),
                        100L);

        assertThat(result.getOrderStatus())
                .isEqualTo(OrderStatus.PAID);

        ArgumentCaptor<CreatePaymentRequestDto> requestCaptor =
                ArgumentCaptor.forClass(CreatePaymentRequestDto.class);

        verify(paymentHttpClient)
                .createPayment(requestCaptor.capture());

        CreatePaymentRequestDto paymentRequest =
                requestCaptor.getValue();

        assertThat(paymentRequest.orderId())
                .isEqualTo(1L);

        assertThat(paymentRequest.paymentMethod())
                .isEqualTo(PaymentMethod.CARD);

        assertThat(paymentRequest.amount())
                .isEqualByComparingTo("25.00");

        verify(orderRepository)
                .save(order);

        ArgumentCaptor<OrderPaidEventDto> eventCaptor =
                ArgumentCaptor.forClass(OrderPaidEventDto.class);

        verify(kafkaTemplate).send(
                        eq("order-paid"),
                        eq("1"),
                        eventCaptor.capture());

        OrderPaidEventDto event =
                eventCaptor.getValue();

        assertThat(event.orderId())
                .isEqualTo(1L);

        assertThat(event.address())
                .isEqualTo("Test address");

        assertThat(event.amount())
                .isEqualByComparingTo("25.00");
    }

    @Test
    void processPayment_card_failure_shouldMarkOrderAsPaymentFailed() {
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        order.setTotalAmount(new BigDecimal("25.00"));

        CreatePaymentResponseDto paymentResponse = new CreatePaymentResponseDto(
                        5L,
                        1L,
                        new BigDecimal("25.00"),
                        PaymentStatus.PAYMENT_FAILED,
                        PaymentMethod.CARD);

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(paymentHttpClient.createPayment(any(CreatePaymentRequestDto.class)))
                .thenReturn(paymentResponse);

        when(orderRepository.save(order))
                .thenReturn(order);

        Order result = orderService.processPayment(
                        1L,
                        new OrderPaymentRequestDto(PaymentMethod.CARD),
                        100L);

        assertThat(result.getOrderStatus())
                .isEqualTo(OrderStatus.PAYMENT_FAILED);

        verify(paymentHttpClient)
                .createPayment(any(CreatePaymentRequestDto.class));

        verify(orderRepository)
                .save(order);

        verify(kafkaTemplate, never())
                .send(
                        anyString(),
                        anyString(),
                        any()
                );
    }

    @Test
    void processPayment_shouldRejectAnotherCustomersOrder() {
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT);

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.processPayment(
                        1L,
                        new OrderPaymentRequestDto(PaymentMethod.CARD),
                        999L))
                .isInstanceOf(OrderAccessDeniedException.class)
                .hasMessage("You can only pay for your own orders");

        verify(orderRepository)
                .findById(1L);

        verify(orderRepository, never())
                .save(any(Order.class));

        verifyNoInteractions(paymentHttpClient);
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void processPayment_shouldRejectOrderWithWrongStatus() {
        order.setOrderStatus(OrderStatus.PAID);

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.processPayment(
                        1L,
                        new OrderPaymentRequestDto(PaymentMethod.CARD),
                        100L))
                .isInstanceOf(InvalidOrderStatusException.class)
                .hasMessage("Order must be in orderStatus PENDING_PAYMENT");

        verify(orderRepository)
                .findById(1L);

        verify(orderRepository, never())
                .save(any(Order.class));

        verifyNoInteractions(paymentHttpClient);
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void processPayment_shouldThrowWhenOrderDoesNotExist() {
        when(orderRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.processPayment(
                        99L,
                        new OrderPaymentRequestDto(PaymentMethod.CARD),
                        100L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessage("Entity with id `99` not found");

        verify(orderRepository)
                .findById(99L);

        verify(orderRepository, never())
                .save(any(Order.class));

        verifyNoInteractions(paymentHttpClient);
        verifyNoInteractions(kafkaTemplate);
    }

    // getOrderOrThrow()

    @Test
    void getOrderOrThrow_shouldReturnOrderWhenExists() {
        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        Order result =
                orderService.getOrderOrThrow(1L);

        assertThat(result)
                .isSameAs(order);

        verify(orderRepository)
                .findById(1L);

        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void getOrderOrThrow_shouldThrowWhenOrderDoesNotExist() {
        when(orderRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderOrThrow(99L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessage("Entity with id `99` not found");

        verify(orderRepository)
                .findById(99L);

        verifyNoMoreInteractions(orderRepository);
    }

    // getAllOrdersByUserId()

    @Test
    void getAllOrdersByUserId_shouldReturnOrdersForCustomer() {
        Order secondOrder = new Order();
        secondOrder.setId(2L);
        secondOrder.setCustomerId(100L);

        when(orderRepository.findAllByCustomerId(100L))
                .thenReturn(List.of(order, secondOrder));

        List<Order> result =
                orderService.getAllOrdersByUserId(100L);

        assertThat(result)
                .containsExactly(order, secondOrder);

        verify(orderRepository)
                .findAllByCustomerId(100L);

        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void getAllOrdersByUserId_shouldReturnEmptyListWhenCustomerHasNoOrders() {
        when(orderRepository.findAllByCustomerId(100L))
                .thenReturn(List.of());

        List<Order> result =
                orderService.getAllOrdersByUserId(100L);

        assertThat(result)
                .isEmpty();

        verify(orderRepository)
                .findAllByCustomerId(100L);

        verifyNoMoreInteractions(orderRepository);
    }
}
