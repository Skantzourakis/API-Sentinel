package com.apisentinel.dto.auth;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterRequestTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private List<String> invalidFields(String password) {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("user@example.com");
        request.setPassword(password);
        return VALIDATOR.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .toList();
    }

    // Regression test: 40 Greek letters are 80 bytes, passed a 72-character check and made BCrypt fail with a 500.
    @Test
    void passwordLimitCountsBytesNotCharacters() {
        assertThat(invalidFields("κ".repeat(36))).isEmpty();
        assertThat(invalidFields("κ".repeat(37))).containsExactly("password");
        assertThat(invalidFields("a".repeat(72))).isEmpty();
        assertThat(invalidFields("a".repeat(73))).containsExactly("password");
    }

    @Test
    void passwordNeedsAtLeastEightCharacters() {
        assertThat(invalidFields("short")).containsExactly("password");
    }
}
