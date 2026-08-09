package com.example.agenticai.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Chat request optionally accepts a model override.
 * If `model` is set to "GEMINI" or "OPENAI", the agent will prefer that model.
 */
public record ChatRequest(
        @NotBlank(message = "message must not be blank") String message,
        String model
) {
}
