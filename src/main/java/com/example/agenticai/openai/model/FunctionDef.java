package com.example.agenticai.openai.model;

import java.util.Map;

/** name/description guide the model's choice; parameters is a JSON-schema object. */
public record FunctionDef(String name, String description, Map<String, Object> parameters) {
}
