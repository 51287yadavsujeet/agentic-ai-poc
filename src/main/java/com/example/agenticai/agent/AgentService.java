package com.example.agenticai.agent;

import com.example.agenticai.agent.tool.ToolRegistry;
import com.example.agenticai.config.ModelSelector;
import com.example.agenticai.dto.ChatResponse;
import com.example.agenticai.openai.GeminiClient;
import com.example.agenticai.openai.OpenAiClient;
import com.example.agenticai.openai.model.ChatCompletionResponse;
import com.example.agenticai.openai.model.ChatMessage;
import com.example.agenticai.openai.model.ToolCall;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * The heart of the POC. This is what makes the app "agentic" rather than a simple chat
 * wrapper: given a user goal, it repeatedly
 *
 *   1) asks the model what to do next (via ModelSelector choosing OpenAI or Gemini),
 *   2) if the model requests a tool, executes that tool itself and feeds the result back,
 *   3) repeats until the model is satisfied it can answer directly (or a safety cap is hit).
 *
 * The model decides which tools to call, in which order, and when it has enough
 * information to stop - Spring code here never hardcodes that plan.
 */
@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);
    private static final int MAX_ITERATIONS = 6;

    private static final String SYSTEM_PROMPT = """
            You are an autonomous assistant with access to tools. Break the user's request
            down, call tools whenever you need real data or a computation instead of
            guessing, and only give a final answer once you are confident it is correct.
            Be concise in your final answer.
            """;

    private final OpenAiClient openAiClient;
    private final GeminiClient geminiClient;
    private final ModelSelector modelSelector;
    private final ToolRegistry toolRegistry;
    private final ExecutorService virtualThreadExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AgentService(OpenAiClient openAiClient, GeminiClient geminiClient, 
                       ModelSelector modelSelector, ToolRegistry toolRegistry, 
                       ExecutorService virtualThreadExecutor) {
        this.openAiClient = openAiClient;
        this.geminiClient = geminiClient;
        this.modelSelector = modelSelector;
        this.toolRegistry = toolRegistry;
        this.virtualThreadExecutor = virtualThreadExecutor;
    }

    public ChatResponse run(String userMessage) {
        return run(userMessage, null);
    }

    /**
     * Run the agent with an optional model override. If modelOverride is set to "GEMINI" or "OPENAI",
     * it will be used; otherwise ModelSelector determines the model.
     */
    public ChatResponse run(String userMessage, String modelOverride) {
        log.info("[AGENT_ORCHESTRATION] ========== AGENT RUN STARTED ==========");
        log.info("[AGENT_ORCHESTRATION] User Request: '{}' | virtualThread={} | threadName={}",
                userMessage, Thread.currentThread().isVirtual(), Thread.currentThread().getName());
        
        // Determine selected model (override > selector)
        ModelSelector.ModelType selectedModel;
        if (modelOverride != null && !modelOverride.isBlank()) {
            try {
                selectedModel = ModelSelector.ModelType.valueOf(modelOverride.trim().toUpperCase());
                log.info("[AGENT_ORCHESTRATION] Model override provided: {}", selectedModel);
            } catch (IllegalArgumentException e) {
                log.warn("[AGENT_ORCHESTRATION] Unknown model override '{}', falling back to selector", modelOverride);
                selectedModel = modelSelector.selectModel(userMessage);
            }
        } else {
            selectedModel = modelSelector.selectModel(userMessage);
        }

        log.info("[AGENT_ORCHESTRATION] Model Selected: {} | Reason: {}",
                selectedModel, selectedModel == ModelSelector.ModelType.GEMINI ? 
                "Keywords detected (Math/History/Geography/Medical)" : "Default OpenAI");
        
        // usedModel reflects the model that actually handled the request (may change if we fallback)
        ModelSelector.ModelType usedModel = selectedModel;
        
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system(SYSTEM_PROMPT));
        messages.add(ChatMessage.user(userMessage));

        List<AgentStep> steps = new ArrayList<>();
        int totalToolCalls = 0;

        for (int iteration = 1; iteration <= MAX_ITERATIONS; iteration++) {
            log.info("[AGENT_LOOP] ========== ITERATION {} START ==========", iteration);
            log.info("[AGENT_LOOP] Iteration {} | messageCount={} | totalToolCalls={} | virtualThread={}",
                    iteration, messages.size(), totalToolCalls, Thread.currentThread().isVirtual());
            
            log.debug("[AGENT_DECISION] Calling {} model to determine next action...", selectedModel);
            ChatCompletionResponse response = null;
            // Try preferred model, fall back to the other if it fails
            if (selectedModel == ModelSelector.ModelType.GEMINI) {
                try {
                    response = geminiClient.chatCompletion(messages, toolRegistry.toolDefinitions());
                } catch (Exception e) {
                    log.warn("[AGENT_ORCHESTRATION] Gemini call failed: {}. Falling back to OpenAI.", e.getMessage());
                    log.info("\u001B[32m[FALLBACK ALERT] from=GEMINI to=OPENAI reason='{}' userMessage='{}'\u001B[0m", e.getMessage(), userMessage);
                    try {
                        response = openAiClient.chatCompletion(messages, toolRegistry.toolDefinitions());
                        usedModel = ModelSelector.ModelType.OPENAI;
                    } catch (Exception ex) {
                        log.error("\u001B[31m[FALLBACK FAILED] OpenAI fallback also failed: {}\u001B[0m", ex.getMessage(), ex);
                        throw ex;
                    }
                }
            } else {
                try {
                    response = openAiClient.chatCompletion(messages, toolRegistry.toolDefinitions());
                } catch (Exception e) {
                    log.warn("[AGENT_ORCHESTRATION] OpenAI call failed: {}. Attempting Gemini as fallback.", e.getMessage());
                    log.info("\u001B[32m[FALLBACK ALERT] from=OPENAI to=GEMINI reason='{}' userMessage='{}'\u001B[0m", e.getMessage(), userMessage);
                    try {
                        response = geminiClient.chatCompletion(messages, toolRegistry.toolDefinitions());
                        usedModel = ModelSelector.ModelType.GEMINI;
                    } catch (Exception ex) {
                        log.error("\u001B[31m[FALLBACK FAILED] Gemini fallback also failed: {}\u001B[0m", ex.getMessage(), ex);
                        throw ex;
                    }
                }
            }
            ChatCompletionResponse.Choice choice = response.choices().get(0);
            ChatMessage assistantMessage = choice.message();
            messages.add(assistantMessage);

            log.info("[AGENT_DECISION] Model Response Received | finishReason='{}' | stopReason indicates: {}",
                    choice.finishReason(),
                    "tool_calls".equals(choice.finishReason()) ? "MORE_WORK_NEEDED" : "FINAL_ANSWER");

            if ("tool_calls".equals(choice.finishReason()) && assistantMessage.toolCalls() != null) {
                log.info("[AGENT_DECISION] *** DECISION MADE: Execute Tools ***");
                log.info("[AGENT_DECISION] Iteration {} will call {} tool(s)", iteration, assistantMessage.toolCalls().size());
                
                int currentIteration = iteration;
                List<Future<ToolExecutionResult>> futures = new ArrayList<>();
                
                for (ToolCall call : assistantMessage.toolCalls()) {
                    log.info("[AGENT_DECISION] Tool selected: '{}' with ID: '{}'", 
                            call.function().name(), call.id());
                    futures.add(virtualThreadExecutor.submit(() -> executeToolCall(currentIteration, call)));
                }

                log.info("[AGENT_EXECUTION] Waiting for {} tool execution(s) to complete...", futures.size());
                for (Future<ToolExecutionResult> future : futures) {
                    ToolExecutionResult executionResult = waitForToolResult(future);
                    totalToolCalls++;

                    log.info("[AGENT_EXECUTION] Tool Result Added | tool='{}' | toolCallId='{}' | resultLength={}",
                            executionResult.toolName(), executionResult.toolCallId(), executionResult.result().length());

                    steps.add(new AgentStep(
                            iteration,
                            "tool_call",
                            "The model requested tool '" + executionResult.toolName() + "' to gather information needed for the final answer.",
                            executionResult.toolName(),
                            executionResult.arguments(),
                            executionResult.result()
                    ));
                    messages.add(ChatMessage.toolResult(
                            executionResult.toolCallId(),
                            executionResult.toolName(),
                            executionResult.result()
                    ));
                }
                log.info("[AGENT_LOOP] Iteration {} completed | Feeding tool results back to model...", iteration);
                continue;
            }

            log.info("[AGENT_DECISION] *** DECISION MADE: Return Final Answer ***");
            log.info("[AGENT_COMPLETION] Final answer produced at iteration {} | totalIterations={} | totalToolCalls={}",
                    iteration, iteration, totalToolCalls);
            
            steps.add(new AgentStep(
                    iteration,
                    "final_answer",
                    "The model had enough information and returned the final human-readable answer.",
                    null,
                    null,
                    assistantMessage.content()
            ));
            
            ChatResponse finalResponse = new ChatResponse(
                    "success",
                    userMessage,
                    assistantMessage.content(),
                    "Completed successfully after " + iteration + " iteration(s) with " + totalToolCalls + " tool call(s).",
                    usedModel.name(),
                    iteration,
                    totalToolCalls,
                    steps
            );
            
            log.info("[AGENT_ORCHESTRATION] ========== AGENT RUN COMPLETED SUCCESSFULLY ==========");
            log.info("[AGENT_ORCHESTRATION] Result | status='{}' | iterations={} | toolCalls={} | answerLength={}",
                    finalResponse.status(), iteration, totalToolCalls, assistantMessage.content().length());
            
            return finalResponse;
        }

        log.warn("[AGENT_ORCHESTRATION] *** SAFETY LIMIT REACHED *** Agent stopped after {} iterations without a final answer.",
                MAX_ITERATIONS);
        log.info("[AGENT_ORCHESTRATION] ========== AGENT RUN INCOMPLETE ==========");
        log.info("[AGENT_ORCHESTRATION] Result | status='incomplete' | maxIterations={} | totalToolCalls={}",
                MAX_ITERATIONS, totalToolCalls);
        
        return new ChatResponse(
                "incomplete",
                userMessage,
                "Stopped after " + MAX_ITERATIONS + " iterations without a final answer.",
                "The agent reached the safety limit before producing a final answer.",
                usedModel.name(),
                MAX_ITERATIONS,
                totalToolCalls,
                steps
        );
    }

    private ToolExecutionResult executeToolCall(int iteration, ToolCall call) {
        String toolName = call.function().name();
        String toolCallId = call.id();
        Map<String, Object> args = parseArguments(call.function().arguments());
        
        log.info("[TOOL_CALL] ========== EXECUTING TOOL ==========");
        log.info("[TOOL_CALL] Iteration {} | Tool: '{}' | ToolCallId: '{}'",
                iteration, toolName, toolCallId);
        log.info("[TOOL_CALL] Execution Context | thread='{}' | virtualThread={} | arguments={}",
                Thread.currentThread().getName(),
                Thread.currentThread().isVirtual(),
                args);

        String result = toolRegistry.execute(toolName, args);

        log.info("[TOOL_CALL] Tool Execution Result | Iteration {} | Tool: '{}' | resultLength={} | resultPreview='{}'",
                iteration,
                toolName,
                result.length(),
                result.substring(0, Math.min(100, result.length())));
        
        log.info("[TOOL_CALL] Execution completed on thread='{}' virtualThread={}",
                Thread.currentThread().getName(),
                Thread.currentThread().isVirtual());

        return new ToolExecutionResult(toolCallId, toolName, args, result);
    }

    private ToolExecutionResult waitForToolResult(Future<ToolExecutionResult> future) {
        log.debug("[TOOL_ASYNC] Waiting for tool execution result on thread: {}", 
                Thread.currentThread().getName());
        try {
            ToolExecutionResult result = future.get();
            log.debug("[TOOL_ASYNC] Successfully retrieved tool execution result for tool: {}", 
                    result.toolName());
            return result;
        } catch (InterruptedException e) {
            log.error("[TOOL_ASYNC] Interrupted while waiting for tool execution", e);
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for virtual-thread tool execution", e);
        } catch (ExecutionException e) {
            log.error("[TOOL_ASYNC] Tool execution failed with exception: {}", e.getMessage(), e);
            throw new IllegalStateException("Virtual-thread tool execution failed", e.getCause());
        }
    }

    private Map<String, Object> parseArguments(String argumentsJson) {
        try {
            return objectMapper.readValue(argumentsJson, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse tool arguments '{}': {}", argumentsJson, e.getMessage());
            return Map.of();
        }
    }

    private record ToolExecutionResult(
            String toolCallId,
            String toolName,
            Map<String, Object> arguments,
            String result
    ) {
    }
}
