package com.example.agenticai.openai.model;

/** name = tool name, arguments = raw JSON string the model produced. */
public record FunctionCall(String name, String arguments) {
}
