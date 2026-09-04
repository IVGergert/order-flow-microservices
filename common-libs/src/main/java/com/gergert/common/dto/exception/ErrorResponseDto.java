package com.gergert.common.dto.exception;

import java.util.Map;

public record ErrorResponseDto(
        int status,
        String message,
        Map<String, String> errors) {}
