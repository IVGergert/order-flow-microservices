package com.gergert.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(
        @NotBlank(message = "Email cannot be empty")
        @Email(message = "Incorrect format email")
        String email,

        @NotBlank(message = "Password cannot be empty")
        String password
) {}
