package com.gergert.authservice.dto;

import com.gergert.authservice.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequestDto(

        @NotBlank(message = "Email cannot be empty")
        @Email(message = "Incorrect format email")
        String email,

        @NotBlank(message = "Password cannot be empty")
        @Size(min = 6, max = 16, message = "Password must be between 6 and 16 characters long.")
        String password,

        @NotBlank(message = "Confirm password cannot be empty")
        @Size(min = 6, max = 16, message = "Confirm password must be between 6 and 16 characters long.")
        String confirmPassword,

        @NotNull(message = "Required role to select (ROLE_CUSTOMER or ROLE_COURIER)")
        Role role

) {}
