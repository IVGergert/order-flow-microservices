package com.gergert.authservice.validation;

import com.gergert.authservice.dto.LoginRequestDto;
import com.gergert.authservice.dto.RegisterRequestDto;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequestDtoValidationTest {
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void login_shouldRejectBlankEmailAndPassword() {
        var violations = validator.validate(new LoginRequestDto("", ""));
        assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                .containsExactlyInAnyOrder("email", "password");
    }

    @Test
    void login_shouldRejectInvalidEmail() {
        var violations = validator.validate(new LoginRequestDto("wrong-email", "password"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    void register_shouldRejectShortPassword() {
        var violations = validator.validate(new RegisterRequestDto("user@example.com", "123", "123"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("confirmPassword"));
    }

    @Test
    void register_shouldAcceptValidRequest() {
        var violations = validator.validate(new RegisterRequestDto("user@example.com", "password", "password"));
        assertThat(violations).isEmpty();
    }
}
