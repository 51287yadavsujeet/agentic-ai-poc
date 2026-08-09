package com.example.agenticai.agent;

import java.util.Map;

/**
 * One recorded step of the agent's reasoning trace, returned to the caller so the whole
 * decision process - not just the final answer - is visible (this is what makes it
 * "agentic" rather than a single request/response call).
 */
public record AgentStep(
        int iteration,
        String stepType,                 // "tool_call" or "final_answer"
        String description,              // human-readable explanation of this step
        String toolName,                 // null for final_answer
        Map<String, Object> toolArguments, // parsed tool arguments, null for final_answer
        String output                    // tool result, or the model's final answer text
) {
}
