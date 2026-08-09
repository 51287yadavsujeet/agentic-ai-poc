package com.example.agenticai.openai.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatCompletionRequest(
        String model,
        List<ChatMessage> messages,
        List<ToolDefinition> tools,
        @JsonProperty("tool_choice") String toolChoice,
        Double temperature
) {
}
