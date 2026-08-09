package com.example.agenticai.openai.model;

/** Advertises a callable tool to the model ("type" is always "function"). */
public record ToolDefinition(String type, FunctionDef function) {

    public static ToolDefinition function(FunctionDef function) {
        return new ToolDefinition("function", function);
    }
}
