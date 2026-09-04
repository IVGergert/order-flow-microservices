package com.gergert.deliveryservice.service.impl;

import com.gergert.deliveryservice.entity.Courier;
import com.gergert.deliveryservice.entity.CourierStatus;
import com.gergert.deliveryservice.repository.CourierRepository;
import com.gergert.deliveryservice.exception.CourierNotAvailableException;
import com.gergert.deliveryservice.exception.CourierNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourierServiceImplTest {
    @Mock private CourierRepository courierRepository;
    @InjectMocks private CourierServiceImpl service;

    @Test
    void goOnline_shouldChangeOfflineToAvailableAndSave() {
        Courier courier = courier(1L, 10L, CourierStatus.OFFLINE);
        when(courierRepository.findByUserId(10L)).thenReturn(Optional.of(courier));
        when(courierRepository.save(courier)).thenReturn(courier);

        var response = service.goOnline(10L);

        assertThat(response.status()).isEqualTo(CourierStatus.AVAILABLE);
        assertThat(courier.getCourierStatus()).isEqualTo(CourierStatus.AVAILABLE);
        verify(courierRepository).save(courier);
    }

    @Test
    void goOnline_shouldNotSaveAlreadyAvailableCourier() {
        Courier courier = courier(1L, 10L, CourierStatus.AVAILABLE);
        when(courierRepository.findByUserId(10L)).thenReturn(Optional.of(courier));

        assertThat(service.goOnline(10L).status()).isEqualTo(CourierStatus.AVAILABLE);
        verify(courierRepository, never()).save(any());
    }

    @Test
    void goOffline_shouldChangeAvailableToOffline() {
        Courier courier = courier(1L, 10L, CourierStatus.AVAILABLE);
        when(courierRepository.findByUserId(10L)).thenReturn(Optional.of(courier));
        when(courierRepository.save(courier)).thenReturn(courier);

        assertThat(service.goOffline(10L).status()).isEqualTo(CourierStatus.OFFLINE);
        verify(courierRepository).save(courier);
    }

    @Test
    void goOffline_shouldRejectCourierWithActiveDelivery() {
        Courier courier = courier(1L, 10L, CourierStatus.ON_THE_WAY_TO_CUSTOMER);
        when(courierRepository.findByUserId(10L)).thenReturn(Optional.of(courier));

        assertThatThrownBy(() -> service.goOffline(10L))
                .isInstanceOf(CourierNotAvailableException.class);
        verify(courierRepository, never()).save(any());
    }

    @Test
    void getStatus_shouldReturnCurrentStatus() {
        Courier courier = courier(1L, 10L, CourierStatus.AVAILABLE);
        when(courierRepository.findByUserId(10L)).thenReturn(Optional.of(courier));

        assertThat(service.getStatus(10L).status()).isEqualTo(CourierStatus.AVAILABLE);
    }

    @Test
    void getStatus_shouldReturnNotFoundWhenCourierDoesNotExist() {
        when(courierRepository.findByUserId(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStatus(10L))
                .isInstanceOf(CourierNotFoundException.class);
    }

    private Courier courier(Long id, Long userId, CourierStatus status) {
        return Courier.builder().id(id).userId(userId).name("Alex").courierStatus(status).build();
    }
}
