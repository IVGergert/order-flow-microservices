package com.gergert.deliveryservice.service.impl;

import com.gergert.common.dto.kafka.OrderPaidEventDto;
import com.gergert.deliveryservice.dto.DeliveryMapper;
import com.gergert.deliveryservice.dto.DeliveryResponseDto;
import com.gergert.deliveryservice.entity.*;
import com.gergert.deliveryservice.exception.*;
import com.gergert.deliveryservice.repository.CourierRepository;
import com.gergert.deliveryservice.repository.DeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceImplTest {
    @Mock private DeliveryRepository deliveryRepository;
    @Mock private CourierRepository courierRepository;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private DeliveryMapper deliveryMapper;
    @InjectMocks private DeliveryServiceImpl service;

    private Courier courier;
    private Delivery delivery;
    private DeliveryResponseDto response;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "deliveryEventsTopic", "delivery-assigned");
        ReflectionTestUtils.setField(service, "orderPickedUpTopic", "order-picked-up");
        ReflectionTestUtils.setField(service, "deliveryCompletedTopic", "delivery-completed");

        courier = Courier.builder()
                .id(7L).userId(100L).name("Alex")
                .courierStatus(CourierStatus.AVAILABLE)
                .transportType(TransportType.CAR)
                .build();

        delivery = Delivery.builder()
                .id(1L).orderId(50L).address("Test address")
                .etaMinutes(30).deliveryStatus(DeliveryStatus.WAITING_FOR_COURIER)
                .build();

        response = DeliveryResponseDto.builder()
                .id(1L).orderId(50L).deliveryStatus(DeliveryStatus.WAITING_FOR_COURIER)
                .address("Test address").etaMinutes(30).build();
    }

    @Test
    void createDelivery_shouldCreateWaitingDelivery() {
        when(deliveryRepository.findByOrderId(50L)).thenReturn(Optional.empty());
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createDelivery(new OrderPaidEventDto(50L, "Test address", new BigDecimal("25.00")));

        var captor = org.mockito.ArgumentCaptor.forClass(Delivery.class);
        verify(deliveryRepository).save(captor.capture());
        Delivery saved = captor.getValue();
        assertThat(saved.getOrderId()).isEqualTo(50L);
        assertThat(saved.getAddress()).isEqualTo("Test address");
        assertThat(saved.getDeliveryStatus()).isEqualTo(DeliveryStatus.WAITING_FOR_COURIER);
        assertThat(saved.getEtaMinutes()).isBetween(20, 59);
    }

    @Test
    void createDelivery_shouldIgnoreDuplicateOrder() {
        when(deliveryRepository.findByOrderId(50L)).thenReturn(Optional.of(delivery));

        service.createDelivery(new OrderPaidEventDto(50L, "Test address", new BigDecimal("25.00")));

        verify(deliveryRepository, never()).save(any());
    }

    @Test
    void acceptDelivery_shouldAssignAvailableCourierAndPublishEvent() {
        when(deliveryRepository.findByOrderIdForUpdate(50L)).thenReturn(Optional.of(delivery));
        when(courierRepository.findByUserId(100L)).thenReturn(Optional.of(courier));
        when(deliveryRepository.save(delivery)).thenReturn(delivery);
        when(courierRepository.save(courier)).thenReturn(courier);
        when(deliveryMapper.toDeliveryDto(delivery)).thenReturn(response);

        assertThat(service.acceptDelivery(50L, 100L)).isEqualTo(response);
        assertThat(delivery.getCourier()).isSameAs(courier);
        assertThat(delivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.COURIER_ASSIGNED);
        assertThat(courier.getCourierStatus()).isEqualTo(CourierStatus.ON_THE_WAY_TO_RESTAURANT);
        verify(kafkaTemplate).send(eq("delivery-assigned"), eq("50"), any());
    }

    @Test
    void acceptDelivery_shouldRejectUnavailableCourier() {
        courier.setCourierStatus(CourierStatus.ON_THE_WAY_TO_CUSTOMER);
        when(deliveryRepository.findByOrderIdForUpdate(50L)).thenReturn(Optional.of(delivery));
        when(courierRepository.findByUserId(100L)).thenReturn(Optional.of(courier));

        assertThatThrownBy(() -> service.acceptDelivery(50L, 100L))
                .isInstanceOf(CourierNotAvailableException.class);
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    void acceptDelivery_shouldRejectAlreadyAcceptedDelivery() {
        delivery.setDeliveryStatus(DeliveryStatus.COURIER_ASSIGNED);
        when(deliveryRepository.findByOrderIdForUpdate(50L)).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> service.acceptDelivery(50L, 100L))
                .isInstanceOf(InvalidDeliveryStatusException.class);
        verifyNoInteractions(courierRepository);
    }

    @Test
    void acceptDelivery_shouldReturnNotFoundWhenDeliveryMissing() {
        when(deliveryRepository.findByOrderIdForUpdate(50L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.acceptDelivery(50L, 100L))
                .isInstanceOf(DeliveryNotFoundException.class);
    }

    @Test
    void pickUpOrder_shouldChangeStatusesAndPublishEvent() {
        delivery.setDeliveryStatus(DeliveryStatus.COURIER_ASSIGNED);
        delivery.setCourier(courier);
        when(deliveryRepository.findByOrderId(50L)).thenReturn(Optional.of(delivery));
        when(deliveryMapper.toDeliveryDto(delivery)).thenReturn(response);

        assertThat(service.pickUpOrder(50L, 100L)).isEqualTo(response);
        assertThat(delivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.PICKED_UP);
        assertThat(courier.getCourierStatus()).isEqualTo(CourierStatus.ON_THE_WAY_TO_CUSTOMER);
        verify(deliveryRepository).save(delivery);
        verify(courierRepository).save(courier);
        verify(kafkaTemplate).send(eq("order-picked-up"), eq("50"), any());
    }

    @Test
    void pickUpOrder_shouldRejectAnotherCourier() {
        delivery.setDeliveryStatus(DeliveryStatus.COURIER_ASSIGNED);
        delivery.setCourier(courier);
        when(deliveryRepository.findByOrderId(50L)).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> service.pickUpOrder(50L, 999L))
                .isInstanceOf(DeliveryAccessDeniedException.class);
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    void pickUpOrder_shouldRejectWrongStatus() {
        delivery.setCourier(courier);
        when(deliveryRepository.findByOrderId(50L)).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> service.pickUpOrder(50L, 100L))
                .isInstanceOf(InvalidDeliveryStatusException.class);
    }

    @Test
    void completeDelivery_shouldMarkDeliveredAndResetCourier() {
        delivery.setDeliveryStatus(DeliveryStatus.PICKED_UP);
        delivery.setCourier(courier);
        when(deliveryRepository.findByOrderId(50L)).thenReturn(Optional.of(delivery));
        when(deliveryMapper.toDeliveryDto(delivery)).thenReturn(response);

        service.completeDelivery(50L, 100L);

        assertThat(delivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(delivery.getCompletedAt()).isNotNull();
        assertThat(courier.getCourierStatus()).isEqualTo(CourierStatus.AVAILABLE);
        verify(deliveryRepository).save(delivery);
        verify(courierRepository).save(courier);
        verify(kafkaTemplate).send(eq("delivery-completed"), eq("50"), any());
    }

    @Test
    void completeDelivery_shouldRejectWrongCourier() {
        delivery.setDeliveryStatus(DeliveryStatus.PICKED_UP);
        delivery.setCourier(courier);
        when(deliveryRepository.findByOrderId(50L)).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> service.completeDelivery(50L, 999L))
                .isInstanceOf(DeliveryAccessDeniedException.class);
    }

    @Test
    void completeDelivery_shouldRejectIfOrderWasNotPickedUp() {
        delivery.setCourier(courier);
        when(deliveryRepository.findByOrderId(50L)).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> service.completeDelivery(50L, 100L))
                .isInstanceOf(InvalidDeliveryStatusException.class);
    }

    @Test
    void getCurrentDelivery_shouldReturnActiveDelivery() {
        delivery.setDeliveryStatus(DeliveryStatus.PICKED_UP);
        delivery.setCourier(courier);
        when(courierRepository.findByUserId(100L)).thenReturn(Optional.of(courier));
        when(deliveryRepository.findFirstByCourier_UserIdAndDeliveryStatusIn(eq(100L), anySet()))
                .thenReturn(Optional.of(delivery));
        when(deliveryMapper.toDeliveryDto(delivery)).thenReturn(response);

        assertThat(service.getCurrentDeliveryByCourierUserId(100L)).contains(response);
    }

    @Test
    void getDeliveriesByCourierUserId_shouldMapAllDeliveries() {
        when(deliveryRepository.findAllByCourier_UserId(100L)).thenReturn(List.of(delivery));
        when(deliveryMapper.toDeliveryDto(delivery)).thenReturn(response);

        assertThat(service.getDeliveriesByCourierUserId(100L)).containsExactly(response);
    }

    @Test
    void getDeliveryByOrderId_shouldRejectAnotherCourier() {
        delivery.setCourier(courier);
        when(deliveryRepository.findByOrderId(50L)).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> service.getDeliveryByOrderId(50L, 999L))
                .isInstanceOf(DeliveryAccessDeniedException.class);
    }

    @Test
    void getWaitingDeliveries_shouldReturnWaitingDeliveriesForAvailableCourier() {
        when(courierRepository.findByUserId(100L)).thenReturn(Optional.of(courier));
        when(deliveryRepository.findAllByDeliveryStatus(DeliveryStatus.WAITING_FOR_COURIER))
                .thenReturn(List.of(delivery));
        when(deliveryMapper.toDeliveryDto(delivery)).thenReturn(response);

        assertThat(service.getWaitingDeliveries(100L)).containsExactly(response);
    }

    @Test
    void getWaitingDeliveries_shouldRejectOfflineCourier() {
        courier.setCourierStatus(CourierStatus.OFFLINE);
        when(courierRepository.findByUserId(100L)).thenReturn(Optional.of(courier));

        assertThatThrownBy(() -> service.getWaitingDeliveries(100L))
                .isInstanceOf(CourierNotAvailableException.class);
        verify(deliveryRepository, never()).findAllByDeliveryStatus(any());
    }

    @Test
    void getCompletedDeliveriesToday_shouldReturnRepositoryCount() {
        when(courierRepository.findByUserId(100L)).thenReturn(Optional.of(courier));
        when(deliveryRepository.countByCourier_UserIdAndCompletedAtBetween(eq(100L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(3L);

        assertThat(service.getCompletedDeliveriesToday(100L).completedToday()).isEqualTo(3L);
    }
}
