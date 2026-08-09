package com.example.agenticai.dto;

import com.example.agenticai.agent.AgentStep;

import java.util.List;

public record ChatResponse(
        String status,
        String userMessage,
        String answer,
        String summary,
        String selectedModel,
        int totalIterations,
        int totalToolCalls,
        List<AgentStep> steps
) {
}
