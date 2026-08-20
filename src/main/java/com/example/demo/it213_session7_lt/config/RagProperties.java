package com.example.demo.it213_session7_lt.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.rag")
public record RagProperties(
        @NotBlank String documentLocation,
        @Min(50) int chunkSize,
        @Min(0) int chunkOverlap,
        @Min(1) int topK,
        @DecimalMin("0.0") @DecimalMax("1.0") double similarityThreshold,
        @Min(2) int maxMemoryMessages) {

    public RagProperties {
        if (chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException("app.rag.chunk-overlap must be smaller than chunk-size");
        }
    }
}
