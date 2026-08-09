package com.example.agenticai.openai.model;

/** A tool invocation requested by the model. */
public record ToolCall(String id, String type, FunctionCall function) {
}
