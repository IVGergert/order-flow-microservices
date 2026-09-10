package com.gergert.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCourierRequestDto(
        @NotBlank(message = "Email cannot be empty")
        @Email(message = "Incorrect format email")
        String email,

        @NotBlank(message = "Password cannot be empty")
        @Size(min = 6, max = 16, message = "Password must be between 6 and 16 characters long.")
        String password,

        @NotBlank(message = "Confirm password cannot be empty")
        @Size(min = 6, max = 16, message = "Confirm password must be between 6 and 16 characters long.")
        String confirmPassword,

        @NotBlank(message = "Name cannot be empty")
        String name,

        @NotBlank(message = "Transport type cannot be empty")
        @Pattern(
                regexp = "BICYCLE|CAR|ON_FOOT",
                message = "Transport type must be BICYCLE, CAR or ON_FOOT"
        )
        String transportType
) {
}
