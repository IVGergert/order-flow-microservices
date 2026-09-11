package com.gergert.deliveryservice.service;

import com.gergert.deliveryservice.dto.CourierStatusResponseDto;

public interface CourierService {
    CourierStatusResponseDto goOnline(Long userId);
    CourierStatusResponseDto goOffline(Long userId);
    CourierStatusResponseDto getStatus(Long userId);
    void validateLogout(Long userId);

}
