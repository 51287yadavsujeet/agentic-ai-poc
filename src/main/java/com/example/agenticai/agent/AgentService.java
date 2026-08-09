package com.example.agenticai.agent;

import com.example.agenticai.agent.tool.ToolRegistry;
import com.example.agenticai.dto.ChatResponse;
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
 *   1) asks the model what to do next,
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
    private final ToolRegistry toolRegistry;
    private final ExecutorService virtualThreadExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AgentService(OpenAiClient openAiClient, ToolRegistry toolRegistry, ExecutorService virtualThreadExecutor) {
        this.openAiClient = openAiClient;
        this.toolRegistry = toolRegistry;
        this.virtualThreadExecutor = virtualThreadExecutor;
    }

    public ChatResponse run(String userMessage) {
        log.info("Agent run started. virtualThread={} userMessage='{}'",
                Thread.currentThread().isVirtual(), userMessage);
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system(SYSTEM_PROMPT));
        messages.add(ChatMessage.user(userMessage));

        List<AgentStep> steps = new ArrayList<>();
        int totalToolCalls = 0;

        for (int iteration = 1; iteration <= MAX_ITERATIONS; iteration++) {
            log.info("Iteration {} started. conversationMessageCount={} virtualThread={}",
                    iteration, messages.size(), Thread.currentThread().isVirtual());
            ChatCompletionResponse response = openAiClient.chatCompletion(messages, toolRegistry.toolDefinitions());
            ChatCompletionResponse.Choice choice = response.choices().get(0);
            ChatMessage assistantMessage = choice.message();
            messages.add(assistantMessage);

            log.info("Iteration {} completed model call. finishReason={}", iteration, choice.finishReason());

            if ("tool_calls".equals(choice.finishReason()) && assistantMessage.toolCalls() != null) {
                log.info("Iteration {} requested {} tool call(s).", iteration, assistantMessage.toolCalls().size());
                int currentIteration = iteration;
                List<Future<ToolExecutionResult>> futures = new ArrayList<>();
                for (ToolCall call : assistantMessage.toolCalls()) {
                    futures.add(virtualThreadExecutor.submit(() -> executeToolCall(currentIteration, call)));
                }

                for (Future<ToolExecutionResult> future : futures) {
                    ToolExecutionResult executionResult = waitForToolResult(future);
                    totalToolCalls++;

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
                // loop again: feed tool results back so the model can decide the next step
                continue;
            }

            // No more tools requested - this is the model's final answer.
            log.info("Final answer produced at iteration {}.", iteration);
            steps.add(new AgentStep(
                    iteration,
                    "final_answer",
                    "The model had enough information and returned the final human-readable answer.",
                    null,
                    null,
                    assistantMessage.content()
            ));
            return new ChatResponse(
                    "success",
                    userMessage,
                    assistantMessage.content(),
                    "Completed successfully after " + iteration + " iteration(s) with " + totalToolCalls + " tool call(s).",
                    iteration,
                    totalToolCalls,
                    steps
            );
        }

        log.warn("Agent stopped after {} iterations without a final answer.", MAX_ITERATIONS);
        return new ChatResponse(
                "incomplete",
                userMessage,
                "Stopped after " + MAX_ITERATIONS + " iterations without a final answer.",
                "The agent reached the safety limit before producing a final answer.",
                MAX_ITERATIONS,
                totalToolCalls,
                steps
        );
    }

    private ToolExecutionResult executeToolCall(int iteration, ToolCall call) {
        String toolName = call.function().name();
        Map<String, Object> args = parseArguments(call.function().arguments());
        log.info("Iteration {} executing tool '{}' on thread='{}' virtualThread={} arguments={}",
                iteration,
                toolName,
                Thread.currentThread().getName(),
                Thread.currentThread().isVirtual(),
                args);

        String result = toolRegistry.execute(toolName, args);

        log.info("Iteration {} tool '{}' completed on thread='{}' virtualThread={} result='{}'",
                iteration,
                toolName,
                Thread.currentThread().getName(),
                Thread.currentThread().isVirtual(),
                result);

        return new ToolExecutionResult(call.id(), toolName, args, result);
    }

    private ToolExecutionResult waitForToolResult(Future<ToolExecutionResult> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for virtual-thread tool execution", e);
        } catch (ExecutionException e) {
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
