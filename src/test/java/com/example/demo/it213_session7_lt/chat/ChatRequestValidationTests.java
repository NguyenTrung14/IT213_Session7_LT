package com.example.demo.it213_session7_lt.chat;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

class ChatRequestValidationTests {

    @Test
    void rejectsBlankSessionAndMessage() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validate(new ChatRequest(" ", ""));

            assertThat(violations).extracting(violation -> violation.getPropertyPath().toString())
                    .containsExactlyInAnyOrder("sessionId", "message");
        }
    }

    @Test
    void acceptsAValidRequest() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validate(
                    new ChatRequest("demo-session", "Laptop bảo hành bao lâu?"));

            assertThat(violations).isEmpty();
        }
    }
}
