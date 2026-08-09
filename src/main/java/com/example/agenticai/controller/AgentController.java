package com.example.agenticai.controller;

import com.example.agenticai.agent.AgentService;
import com.example.agenticai.agent.tool.Tool;
import com.example.agenticai.agent.tool.ToolRegistry;
import com.example.agenticai.dto.ChatRequest;
import com.example.agenticai.dto.ChatResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        log.info("POST /api/agent/chat received. virtualThread={} message='{}'",
                Thread.currentThread().isVirtual(), request.message());
        return ResponseEntity.ok(agentService.run(request.message()));
    }

    /** Browser-friendly endpoint: pass the prompt as a query parameter. */
    @GetMapping("/chat-browser")
    public ResponseEntity<ChatResponse> chatBrowser(@RequestParam String message) {
        log.info("GET /api/agent/chat-browser received. virtualThread={} message='{}'",
                Thread.currentThread().isVirtual(), message);
        return ResponseEntity.ok(agentService.run(message));
    }

    /** Lists the tools currently registered and available to the agent. */
    @GetMapping("/tools")
    public ResponseEntity<List<Map<String, String>>> tools() {
        log.info("GET /api/agent/tools received. virtualThread={}", Thread.currentThread().isVirtual());
        List<Map<String, String>> result = toolRegistry.all().stream()
                .map(t -> Map.of("name", t.name(), "description", t.description()))
                .toList();
        return ResponseEntity.ok(result);
    }
}
