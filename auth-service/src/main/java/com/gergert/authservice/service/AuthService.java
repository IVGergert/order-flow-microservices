package com.gergert.authservice.service;

import com.gergert.authservice.dto.*;

public interface AuthService {
    AuthResponseDto login(LoginRequestDto loginDto);
    AuthResponseDto register(RegisterRequestDto registerDto);
}
