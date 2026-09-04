package com.gergert.deliveryservice.service.impl;

import com.gergert.deliveryservice.dto.CourierStatusResponseDto;
import com.gergert.deliveryservice.entity.Courier;
import com.gergert.deliveryservice.entity.CourierStatus;
import com.gergert.deliveryservice.exception.CourierNotAvailableException;
import com.gergert.deliveryservice.exception.CourierNotFoundException;
import com.gergert.deliveryservice.repository.CourierRepository;
import com.gergert.deliveryservice.service.CourierService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CourierServiceImpl implements CourierService {
    private final CourierRepository courierRepository;

    @Override
    @Transactional
    public CourierStatusResponseDto goOnline(Long userId) {
        Courier courier = getCourierByUserId(userId);

        if (courier.getCourierStatus() == CourierStatus.OFFLINE) {
            courier.setCourierStatus(CourierStatus.AVAILABLE);
            courierRepository.save(courier);
            log.info("Courier userId={} went ONLINE", userId);
        }

        return new CourierStatusResponseDto(courier.getCourierStatus());
    }

    @Override
    @Transactional
    public CourierStatusResponseDto goOffline(Long userId) {
        Courier courier = getCourierByUserId(userId);

        if (courier.getCourierStatus() != CourierStatus.AVAILABLE) {
            throw new CourierNotAvailableException(
                    "Cannot go offline while having an active delivery!"
            );
        }

        courier.setCourierStatus(CourierStatus.OFFLINE);
        courierRepository.save(courier);
        log.info("Courier courierId = {} went OFFLINE", courier.getId());

        return new CourierStatusResponseDto(courier.getCourierStatus());
    }

    @Override
    @Transactional(readOnly = true)
    public CourierStatusResponseDto getStatus(Long userId) {
        Courier courier = getCourierByUserId(userId);
        return new CourierStatusResponseDto(courier.getCourierStatus());
    }

    private Courier getCourierByUserId(Long userId) {
        return courierRepository.findByUserId(userId)
                .orElseThrow(() -> new CourierNotFoundException("Courier not found for userId = " + userId));
    }
}
