package com.gergert.authservice.service;

import com.gergert.authservice.dto.CreateCourierRequestDto;
import com.gergert.authservice.dto.UserResponseDto;

public interface AdminService {
    UserResponseDto createCourier(CreateCourierRequestDto request);
}
