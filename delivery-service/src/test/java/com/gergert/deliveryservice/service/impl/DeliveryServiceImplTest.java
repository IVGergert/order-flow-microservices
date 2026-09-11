package com.gergert.deliveryservice.service.impl;

import com.gergert.common.dto.kafka.DeliveryAssignedEventDto;
import com.gergert.common.dto.kafka.OrderDeliveredEventDto;
import com.gergert.common.dto.kafka.OrderPaidEventDto;
import com.gergert.common.dto.kafka.OrderPickedUpEventDto;
import com.gergert.deliveryservice.dto.CourierStatisticsResponseDto;
import com.gergert.deliveryservice.dto.DeliveryMapper;
import com.gergert.deliveryservice.dto.DeliveryResponseDto;
import com.gergert.deliveryservice.entity.Courier;
import com.gergert.deliveryservice.entity.CourierStatus;
import com.gergert.deliveryservice.entity.Delivery;
import com.gergert.deliveryservice.entity.DeliveryStatus;
import com.gergert.deliveryservice.entity.TransportType;
import com.gergert.deliveryservice.exception.CourierNotAvailableException;
import com.gergert.deliveryservice.exception.CourierNotFoundException;
import com.gergert.deliveryservice.exception.DeliveryAccessDeniedException;
import com.gergert.deliveryservice.exception.DeliveryNotFoundException;
import com.gergert.deliveryservice.exception.InvalidDeliveryStatusException;
import com.gergert.deliveryservice.repository.CourierRepository;
import com.gergert.deliveryservice.repository.DeliveryRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceImplTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private CourierRepository courierRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private DeliveryMapper deliveryMapper;

    @InjectMocks
    private DeliveryServiceImpl service;

    private Courier courier;
    private Delivery delivery;
    private DeliveryResponseDto response;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                service,
                "deliveryEventsTopic",
                "delivery-assigned"
        );

        ReflectionTestUtils.setField(
                service,
                "orderPickedUpTopic",
                "order-picked-up"
        );

        ReflectionTestUtils.setField(
                service,
                "deliveryCompletedTopic",
                "delivery-completed"
        );

        courier = Courier.builder()
                .id(7L)
                .userId(100L)
                .name("Alex")
                .courierStatus(CourierStatus.AVAILABLE)
                .transportType(TransportType.CAR)
                .build();

        delivery = Delivery.builder()
                .id(1L)
                .orderId(50L)
                .address("Test address")
                .etaMinutes(30)
                .deliveryStatus(DeliveryStatus.WAITING_FOR_COURIER)
                .build();

        response = DeliveryResponseDto.builder()
                .id(1L)
                .orderId(50L)
                .deliveryStatus(DeliveryStatus.WAITING_FOR_COURIER)
                .address("Test address")
                .etaMinutes(30)
                .build();
    }

    // createDelivery()

    @Test
    void createDelivery_shouldCreateWaitingDelivery() {
        when(deliveryRepository.findByOrderId(50L))
                .thenReturn(Optional.empty());

        when(deliveryRepository.save(any(Delivery.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrderPaidEventDto event = new OrderPaidEventDto(
                50L,
                "Test address",
                new BigDecimal("25.00")
        );

        service.createDelivery(event);

        ArgumentCaptor<Delivery> captor =
                ArgumentCaptor.forClass(Delivery.class);

        verify(deliveryRepository).save(captor.capture());

        Delivery savedDelivery = captor.getValue();

        assertThat(savedDelivery.getOrderId())
                .isEqualTo(50L);

        assertThat(savedDelivery.getAddress())
                .isEqualTo("Test address");

        assertThat(savedDelivery.getDeliveryStatus())
                .isEqualTo(DeliveryStatus.WAITING_FOR_COURIER);

        assertThat(savedDelivery.getEtaMinutes())
                .isBetween(20, 59);
    }

    @Test
    void createDelivery_shouldNotCreateDuplicateDelivery() {
        when(deliveryRepository.findByOrderId(50L))
                .thenReturn(Optional.of(delivery));

        OrderPaidEventDto event = new OrderPaidEventDto(
                50L,
                "Test address",
                new BigDecimal("25.00")
        );

        service.createDelivery(event);

        verify(deliveryRepository, never())
                .save(any(Delivery.class));
    }

    // acceptDelivery()

    @Test
    void acceptDelivery_shouldAssignAvailableCourier() {
        when(deliveryRepository.findByOrderId(50L))
                .thenReturn(Optional.of(delivery));

        when(courierRepository.findByUserId(100L))
                .thenReturn(Optional.of(courier));

        when(deliveryMapper.toDeliveryDto(delivery))
                .thenReturn(response);

        DeliveryResponseDto result =
                service.acceptDelivery(50L, 100L);

        assertThat(result)
                .isEqualTo(response);

        assertThat(delivery.getCourier())
                .isSameAs(courier);

        assertThat(delivery.getDeliveryStatus())
                .isEqualTo(DeliveryStatus.COURIER_ASSIGNED);

        assertThat(courier.getCourierStatus())
                .isEqualTo(CourierStatus.ON_THE_WAY_TO_RESTAURANT);

        verify(deliveryRepository)
                .save(delivery);

        verify(courierRepository)
                .save(courier);

        verify(kafkaTemplate)
                .send(
                        eq("delivery-assigned"),
                        eq("50"),
                        any(DeliveryAssignedEventDto.class)
                );

        verify(deliveryMapper)
                .toDeliveryDto(delivery);
    }

    @Test
    void acceptDelivery_shouldRejectAlreadyAcceptedDelivery() {
        delivery.setDeliveryStatus(
                DeliveryStatus.COURIER_ASSIGNED
        );

        when(deliveryRepository.findByOrderId(50L))
                .thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> service.acceptDelivery(50L, 100L))
                .isInstanceOf(InvalidDeliveryStatusException.class)
                .hasMessage("Delivery has already been accepted.");

        verifyNoInteractions(courierRepository);
        verifyNoInteractions(kafkaTemplate);
        verifyNoInteractions(deliveryMapper);
    }

    @Test
    void acceptDelivery_shouldRejectUnavailableCourier() {
        courier.setCourierStatus(
                CourierStatus.ON_THE_WAY_TO_CUSTOMER
        );

        when(deliveryRepository.findByOrderId(50L))
                .thenReturn(Optional.of(delivery));

        when(courierRepository.findByUserId(100L))
                .thenReturn(Optional.of(courier));

        assertThatThrownBy(() -> service.acceptDelivery(50L, 100L))
                .isInstanceOf(CourierNotAvailableException.class)
                .hasMessage("Courier is not available.");

        verify(deliveryRepository, never())
                .save(any(Delivery.class));

        verify(courierRepository, never())
                .save(any(Courier.class));

        verifyNoInteractions(kafkaTemplate);
        verifyNoInteractions(deliveryMapper);
    }

    @Test
    void acceptDelivery_shouldThrowWhenDeliveryNotFound() {
        when(deliveryRepository.findByOrderId(50L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.acceptDelivery(50L, 100L))
                .isInstanceOf(DeliveryNotFoundException.class)
                .hasMessage("Delivery not found for orderId=50");

        verifyNoInteractions(courierRepository);
        verifyNoInteractions(kafkaTemplate);
        verifyNoInteractions(deliveryMapper);
    }

    @Test
    void acceptDelivery_shouldThrowWhenCourierNotFound() {
        when(deliveryRepository.findByOrderId(50L))
                .thenReturn(Optional.of(delivery));

        when(courierRepository.findByUserId(100L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> service.acceptDelivery(50L, 100L))
                .isInstanceOf(CourierNotFoundException.class)
                .hasMessage("Courier not found.");

        verify(deliveryRepository, never())
                .save(any(Delivery.class));

        verifyNoInteractions(kafkaTemplate);
        verifyNoInteractions(deliveryMapper);
    }

    // pickUpOrder()

    @Test
    void pickUpOrder_shouldChangeStatusesAndPublishEvent() {
        delivery.setDeliveryStatus(
                DeliveryStatus.COURIER_ASSIGNED
        );

        delivery.setCourier(courier);

        when(deliveryRepository.findByOrderId(50L))
                .thenReturn(Optional.of(delivery));

        when(deliveryMapper.toDeliveryDto(delivery))
                .thenReturn(response);

        DeliveryResponseDto result =
                service.pickUpOrder(50L, 100L);

        assertThat(result)
                .isEqualTo(response);

        assertThat(delivery.getDeliveryStatus())
                .isEqualTo(DeliveryStatus.PICKED_UP);

        assertThat(courier.getCourierStatus())
                .isEqualTo(CourierStatus.ON_THE_WAY_TO_CUSTOMER);

        verify(deliveryRepository)
                .save(delivery);

        verify(courierRepository)
                .save(courier);

        verify(kafkaTemplate)
                .send(
                        eq("order-picked-up"),
                        eq("50"),
                        any(OrderPickedUpEventDto.class)
                );

        verify(deliveryMapper)
                .toDeliveryDto(delivery);
    }

    @Test
    void pickUpOrder_shouldRejectAnotherCourier() {
        delivery.setDeliveryStatus(
                DeliveryStatus.COURIER_ASSIGNED
        );

        delivery.setCourier(courier);

        when(deliveryRepository.findByOrderId(50L))
                .thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> service.pickUpOrder(50L, 999L))
                .isInstanceOf(DeliveryAccessDeniedException.class)
                .hasMessage("You cannot access someone else's delivery!");

        verify(deliveryRepository, never())
                .save(any(Delivery.class));

        verify(courierRepository, never())
                .save(any(Courier.class));

        verifyNoInteractions(kafkaTemplate);
        verifyNoInteractions(deliveryMapper);
    }

    @Test
    void pickUpOrder_shouldRejectWrongStatus() {
        delivery.setCourier(courier);

        when(deliveryRepository.findByOrderId(50L))
                .thenReturn(Optional.of(delivery));

        assertThatThrownBy(
                () -> service.pickUpOrder(50L, 100L))
                .isInstanceOf(InvalidDeliveryStatusException.class)
                .hasMessage("Cannot pick up order with status: WAITING_FOR_COURIER");

        verify(deliveryRepository, never())
                .save(any(Delivery.class));

        verify(courierRepository, never())
                .save(any(Courier.class));

        verifyNoInteractions(kafkaTemplate);
        verifyNoInteractions(deliveryMapper);
    }

    @Test
    void pickUpOrder_shouldRejectDeliveryWithoutCourier() {
        delivery.setDeliveryStatus(
                DeliveryStatus.COURIER_ASSIGNED
        );

        when(deliveryRepository.findByOrderId(50L))
                .thenReturn(Optional.of(delivery));

        assertThatThrownBy(
                () -> service.pickUpOrder(50L, 100L))
                .isInstanceOf(DeliveryAccessDeniedException.class)
                .hasMessage("You cannot access someone else's delivery!");

        verify(deliveryRepository, never())
                .save(any(Delivery.class));

        verify(courierRepository, never())
                .save(any(Courier.class));

        verifyNoInteractions(kafkaTemplate);
        verifyNoInteractions(deliveryMapper);
    }

    @Test
    void pickUpOrder_shouldThrowWhenDeliveryNotFound() {
        when(deliveryRepository.findByOrderId(50L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.pickUpOrder(50L, 100L))
                .isInstanceOf(DeliveryNotFoundException.class);

        verifyNoInteractions(courierRepository);
        verifyNoInteractions(kafkaTemplate);
        verifyNoInteractions(deliveryMapper);
    }

    // completeDelivery()

    @Test
    void completeDelivery_shouldMarkDeliveredAndResetCourier() {
        delivery.setDeliveryStatus(
                DeliveryStatus.PICKED_UP
        );

        delivery.setCourier(courier);

        when(deliveryRepository.findByOrderId(50L))
                .thenReturn(Optional.of(delivery));

        when(deliveryMapper.toDeliveryDto(delivery))
                .thenReturn(response);

        DeliveryResponseDto result =
                service.completeDelivery(50L, 100L);

        assertThat(result)
                .isEqualTo(response);

        assertThat(delivery.getDeliveryStatus())
                .isEqualTo(DeliveryStatus.DELIVERED);

        assertThat(delivery.getCompletedAt())
                .isNotNull();

        assertThat(courier.getCourierStatus())
                .isEqualTo(CourierStatus.AVAILABLE);

        verify(deliveryRepository)
                .save(delivery);

        verify(courierRepository)
                .save(courier);

        verify(kafkaTemplate)
                .send(
                        eq("delivery-completed"),
                        eq("50"),
                        any(OrderDeliveredEventDto.class)
                );

        verify(deliveryMapper)
                .toDeliveryDto(delivery);
    }

    @Test
    void completeDelivery_shouldRejectAnotherCourier() {
        delivery.setDeliveryStatus(
                DeliveryStatus.PICKED_UP
        );

        delivery.setCourier(courier);

        when(deliveryRepository.findByOrderId(50L))
                .thenReturn(Optional.of(delivery));

        assertThatThrownBy(
                () -> service.completeDelivery(50L, 999L))
                .isInstanceOf(DeliveryAccessDeniedException.class)
                .hasMessage("You cannot access someone else's delivery!");

        verify(deliveryRepository, never())
                .save(any(Delivery.class));

        verify(courierRepository, never())
                .save(any(Courier.class));

        verifyNoInteractions(kafkaTemplate);
        verifyNoInteractions(deliveryMapper);
    }

    @Test
    void completeDelivery_shouldRejectIfOrderWasNotPickedUp() {
        delivery.setCourier(courier);

        when(deliveryRepository.findByOrderId(50L))
                .thenReturn(Optional.of(delivery));

        assertThatThrownBy(
                () -> service.completeDelivery(50L, 100L))
                .isInstanceOf(InvalidDeliveryStatusException.class)
                .hasMessage("Cannot complete delivery before picking up order from restaurant!");

        verify(deliveryRepository, never())
                .save(any(Delivery.class));

        verify(courierRepository, never())
                .save(any(Courier.class));

        verifyNoInteractions(kafkaTemplate);
        verifyNoInteractions(deliveryMapper);
    }

    @Test
    void completeDelivery_shouldThrowWhenDeliveryNotFound() {
        when(deliveryRepository.findByOrderId(50L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.completeDelivery(50L, 100L))
                .isInstanceOf(DeliveryNotFoundException.class);

        verifyNoInteractions(courierRepository);
        verifyNoInteractions(kafkaTemplate);
        verifyNoInteractions(deliveryMapper);
    }

    // getCurrentDeliveryByCourierUserId()

    @Test
    void getCurrentDelivery_shouldReturnActiveDelivery() {
        delivery.setDeliveryStatus(
                DeliveryStatus.PICKED_UP
        );

        delivery.setCourier(courier);

        when(deliveryRepository.findFirstByCourier_UserIdAndDeliveryStatusIn(
                                eq(100L),
                                anySet()
                        )).thenReturn(Optional.of(delivery));

        when(deliveryMapper.toDeliveryDto(delivery))
                .thenReturn(response);

        Optional<DeliveryResponseDto> result =
                service.getCurrentDeliveryByCourierUserId(100L);

        assertThat(result)
                .contains(response);

        ArgumentCaptor<Set<DeliveryStatus>> statusCaptor =
                ArgumentCaptor.forClass(Set.class);

        verify(deliveryRepository)
                .findFirstByCourier_UserIdAndDeliveryStatusIn(
                        eq(100L),
                        statusCaptor.capture()
                );

        assertThat(statusCaptor.getValue())
                .containsExactlyInAnyOrder(
                        DeliveryStatus.COURIER_ASSIGNED,
                        DeliveryStatus.PICKED_UP
                );

        verify(deliveryMapper)
                .toDeliveryDto(delivery);
    }

    @Test
    void getCurrentDelivery_shouldReturnEmptyWhenNoActiveDelivery() {
        when(
                deliveryRepository
                        .findFirstByCourier_UserIdAndDeliveryStatusIn(
                                eq(100L),
                                anySet()
                        )
        )
                .thenReturn(Optional.empty());

        Optional<DeliveryResponseDto> result =
                service.getCurrentDeliveryByCourierUserId(100L);

        assertThat(result)
                .isEmpty();

        verifyNoInteractions(deliveryMapper);
    }

    // getDeliveriesByCourierUserId()

    @Test
    void getDeliveriesByCourierUserId_shouldReturnMappedDeliveries() {
        Delivery secondDelivery = Delivery.builder()
                .id(2L)
                .orderId(51L)
                .address("Second address")
                .etaMinutes(25)
                .deliveryStatus(DeliveryStatus.DELIVERED)
                .courier(courier)
                .build();

        DeliveryResponseDto secondResponse =
                DeliveryResponseDto.builder()
                        .id(2L)
                        .orderId(51L)
                        .deliveryStatus(DeliveryStatus.DELIVERED)
                        .address("Second address")
                        .etaMinutes(25)
                        .build();

        when(deliveryRepository.findAllByCourier_UserId(100L))
                .thenReturn(List.of(delivery, secondDelivery));

        when(deliveryMapper.toDeliveryDto(delivery))
                .thenReturn(response);

        when(deliveryMapper.toDeliveryDto(secondDelivery))
                .thenReturn(secondResponse);

        List<DeliveryResponseDto> result =
                service.getDeliveriesByCourierUserId(100L);

        assertThat(result)
                .containsExactly(response, secondResponse);

        verify(deliveryMapper)
                .toDeliveryDto(delivery);

        verify(deliveryMapper)
                .toDeliveryDto(secondDelivery);
    }

    @Test
    void getDeliveriesByCourierUserId_shouldReturnEmptyListWhenNoDeliveries() {
        when(deliveryRepository.findAllByCourier_UserId(100L))
                .thenReturn(List.of());

        List<DeliveryResponseDto> result =
                service.getDeliveriesByCourierUserId(100L);

        assertThat(result)
                .isEmpty();

        verifyNoInteractions(deliveryMapper);
    }

    // getDeliveryByOrderId()

    @Test
    void getDeliveryByOrderId_shouldReturnDeliveryForOwner() {
        delivery.setCourier(courier);

        when(deliveryRepository.findByOrderId(50L))
                .thenReturn(Optional.of(delivery));

        when(deliveryMapper.toDeliveryDto(delivery))
                .thenReturn(response);

        DeliveryResponseDto result =
                service.getDeliveryByOrderId(50L, 100L);

        assertThat(result)
                .isEqualTo(response);

        verify(deliveryMapper)
                .toDeliveryDto(delivery);
    }

    @Test
    void getDeliveryByOrderId_shouldRejectAnotherCourier() {
        delivery.setCourier(courier);

        when(deliveryRepository.findByOrderId(50L))
                .thenReturn(Optional.of(delivery));

        assertThatThrownBy(
                () -> service.getDeliveryByOrderId(50L, 999L))
                .isInstanceOf(DeliveryAccessDeniedException.class)
                .hasMessage("You cannot access someone else's delivery!");

        verifyNoInteractions(deliveryMapper);
    }

    @Test
    void getDeliveryByOrderId_shouldRejectDeliveryWithoutCourier() {
        when(deliveryRepository.findByOrderId(50L))
                .thenReturn(Optional.of(delivery));

        assertThatThrownBy(
                () -> service.getDeliveryByOrderId(50L, 100L))
                .isInstanceOf(DeliveryAccessDeniedException.class)
                .hasMessage("You cannot access someone else's delivery!");

        verifyNoInteractions(deliveryMapper);
    }

    @Test
    void getDeliveryByOrderId_shouldThrowWhenDeliveryNotFound() {
        when(deliveryRepository.findByOrderId(50L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDeliveryByOrderId(50L, 100L))
                .isInstanceOf(DeliveryNotFoundException.class);

        verifyNoInteractions(deliveryMapper);
    }

    // getWaitingDeliveries()

    @Test
    void getWaitingDeliveries_shouldReturnWaitingDeliveriesForAvailableCourier() {
        when(courierRepository.findByUserId(100L))
                .thenReturn(Optional.of(courier));

        when(deliveryRepository.findAllByDeliveryStatus(DeliveryStatus.WAITING_FOR_COURIER))
                .thenReturn(List.of(delivery));

        when(deliveryMapper.toDeliveryDto(delivery))
                .thenReturn(response);

        List<DeliveryResponseDto> result =
                service.getWaitingDeliveries(100L);

        assertThat(result)
                .containsExactly(response);

        verify(deliveryRepository)
                .findAllByDeliveryStatus(DeliveryStatus.WAITING_FOR_COURIER);

        verify(deliveryMapper)
                .toDeliveryDto(delivery);
    }

    @Test
    void getWaitingDeliveries_shouldRejectOfflineCourier() {
        courier.setCourierStatus(
                CourierStatus.OFFLINE
        );

        when(courierRepository.findByUserId(100L))
                .thenReturn(Optional.of(courier));

        assertThatThrownBy(() -> service.getWaitingDeliveries(100L))
                .isInstanceOf(CourierNotAvailableException.class)
                .hasMessage("Courier is not available.");

        verify(deliveryRepository, never())
                .findAllByDeliveryStatus(any(DeliveryStatus.class));

        verifyNoInteractions(deliveryMapper);
    }

    @Test
    void getWaitingDeliveries_shouldThrowWhenCourierNotFound() {
        when(courierRepository.findByUserId(100L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> service.getWaitingDeliveries(100L))
                .isInstanceOf(CourierNotFoundException.class)
                .hasMessage("Courier not found.");

        verify(deliveryRepository, never())
                .findAllByDeliveryStatus(any(DeliveryStatus.class));

        verifyNoInteractions(deliveryMapper);
    }

    // getCompletedDeliveriesToday()

    @Test
    void getCompletedDeliveriesToday_shouldReturnRepositoryCount() {
        when(courierRepository.findByUserId(100L))
                .thenReturn(Optional.of(courier));

        when(deliveryRepository.countByCourier_UserIdAndCompletedAtBetween(
                                eq(100L),
                                any(LocalDateTime.class),
                                any(LocalDateTime.class)
        )).thenReturn(3L);

        CourierStatisticsResponseDto result =
                service.getCompletedDeliveriesToday(100L);

        assertThat(result.completedToday())
                .isEqualTo(3L);

        verify(deliveryRepository).countByCourier_UserIdAndCompletedAtBetween(
                        eq(100L),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class)
                );
    }

    @Test
    void getCompletedDeliveriesToday_shouldReturnZeroWhenNoCompletedDeliveries() {
        when(courierRepository.findByUserId(100L))
                .thenReturn(Optional.of(courier));

        when(deliveryRepository.countByCourier_UserIdAndCompletedAtBetween(
                                eq(100L),
                                any(LocalDateTime.class),
                                any(LocalDateTime.class)
        )).thenReturn(0L);

        CourierStatisticsResponseDto result =
                service.getCompletedDeliveriesToday(100L);

        assertThat(result.completedToday())
                .isZero();
    }

    @Test
    void getCompletedDeliveriesToday_shouldThrowWhenCourierNotFound() {
        when(courierRepository.findByUserId(100L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCompletedDeliveriesToday(100L))
                .isInstanceOf(CourierNotFoundException.class)
                .hasMessage("Courier not found.");

        verify(deliveryRepository, never())
                .countByCourier_UserIdAndCompletedAtBetween(
                        anyLong(),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class)
                );
    }
}