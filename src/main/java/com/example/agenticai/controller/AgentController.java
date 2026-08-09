package com.example.agenticai.controller;

import com.example.agenticai.agent.AgentService;
import com.example.agenticai.agent.tool.Tool;
import com.example.agenticai.agent.tool.ToolRegistry;
import com.example.agenticai.dto.ChatRequest;
import com.example.agenticai.dto.ChatResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    private final AgentService agentService;
    private final ToolRegistry toolRegistry;

    public AgentController(AgentService agentService, ToolRegistry toolRegistry) {
        this.agentService = agentService;
        this.toolRegistry = toolRegistry;
    }

    /** Send a natural-language goal; the agent decides which tools (if any) to call. */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody @Valid ChatRequest request) {
        log.info("[API_REQUEST] ========== POST /api/agent/chat RECEIVED ==========");
        log.info("[API_REQUEST] Request Details | virtualThread={} | message='{}'",
                Thread.currentThread().isVirtual(), request.message());
        log.info("[API_REQUEST] Processing incoming chat request...");
        
        try {
            ChatResponse response = agentService.run(request.message(), request.model());
            log.info("[API_RESPONSE] Chat request completed | status='{}' | model='{}' | iterations={} | toolCalls={}",
                    response.status(), response.selectedModel(), response.totalIterations(), response.totalToolCalls());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[API_ERROR] Error processing chat request: {}", e.getMessage(), e);
            throw e;
        }
    }

    /** Browser-friendly endpoint: pass the prompt as a query parameter. */
    @GetMapping("/chat-browser")
    public ResponseEntity<ChatResponse> chatBrowser(@RequestParam String message, @RequestParam(required = false) String model) {
        log.info("[API_REQUEST] ========== GET /api/agent/chat-browser RECEIVED ==========");
        log.info("[API_REQUEST] Request Details | virtualThread={} | message='{}'",
                Thread.currentThread().isVirtual(), message);
        log.info("[API_REQUEST] Processing incoming browser chat request...");
        
        try {
            ChatResponse response = agentService.run(message, model);
            log.info("[API_RESPONSE] Browser chat request completed | status='{}' | model='{}' | iterations={} | toolCalls={}",
                    response.status(), response.selectedModel(), response.totalIterations(), response.totalToolCalls());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[API_ERROR] Error processing browser chat request: {}", e.getMessage(), e);
            throw e;
        }
    }

    /** Lists the tools currently registered and available to the agent. */
    @GetMapping("/tools")
    public ResponseEntity<List<Map<String, String>>> tools() {
        log.info("[API_REQUEST] ========== GET /api/agent/tools RECEIVED ==========");
        log.info("[API_REQUEST] Request Details | virtualThread={}", Thread.currentThread().isVirtual());
        
        log.info("[API_RESPONSE] Retrieving available tools from registry...");
        List<Map<String, String>> result = toolRegistry.all().stream()
                .map(t -> Map.of("name", t.name(), "description", t.description()))
                .toList();
        
        log.info("[API_RESPONSE] Tools list generated | count={} | tools={}",
                result.size(), result.stream().map(t -> t.get("name")).toList());
        
        return ResponseEntity.ok(result);
    }
}
