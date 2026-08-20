package com.example.demo.it213_session7_lt.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank(message = "sessionId must not be blank")
        @Size(max = 100, message = "sessionId must contain at most 100 characters")
        String sessionId,

        @NotBlank(message = "message must not be blank")
        @Size(max = 2000, message = "message must contain at most 2000 characters")
        String message) {
}
