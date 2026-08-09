package com.example.agenticai.agent.tool;

import com.example.agenticai.openai.model.FunctionDef;
import com.example.agenticai.openai.model.ToolDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Collects every {@link Tool} bean in the application context and exposes them both as
 * OpenAI tool definitions (for the request) and as an executable registry (for the loop).
 */
@Component
public class ToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(ToolRegistry.class);
    private final Map<String, Tool> toolsByName;

    public ToolRegistry(List<Tool> tools) {
        this.toolsByName = tools.stream().collect(Collectors.toMap(Tool::name, t -> t));
        log.info("[TOOL_REGISTRY] Initialized with {} tools: {}", toolsByName.size(), toolsByName.keySet());
    }

    public List<ToolDefinition> toolDefinitions() {
        log.debug("[TOOL_REGISTRY] Building tool definitions for {} available tools", toolsByName.size());
        List<ToolDefinition> definitions = toolsByName.values().stream()
                .map(t -> {
                    log.debug("[TOOL_REGISTRY] Adding tool definition: {}", t.name());
                    return ToolDefinition.function(new FunctionDef(t.name(), t.description(), t.parameters()));
                })
                .toList();
        log.debug("[TOOL_REGISTRY] Tool definitions built successfully. Count={}", definitions.size());
        return definitions;
    }

    public String execute(String toolName, Map<String, Object> arguments) {
        log.info("[TOOL_EXECUTION] Executing tool: toolName='{}' arguments={}", toolName, arguments);
        Tool tool = toolsByName.get(toolName);
        if (tool == null) {
            log.error("[TOOL_EXECUTION] Tool not found: toolName='{}'. Available tools: {}", toolName, toolsByName.keySet());
            return "Error: no tool registered with name '" + toolName + "'";
        }
        try {
            log.info("[TOOL_EXECUTION] Starting execution of tool: {}", toolName);
            String result = tool.execute(arguments);
            log.info("[TOOL_EXECUTION] Tool execution completed successfully: toolName='{}' resultLength={}", 
                    toolName, result.length());
            return result;
        } catch (Exception e) {
            log.error("[TOOL_EXECUTION] Error executing tool: toolName='{}' exception={}", toolName, e.getMessage(), e);
            return "Error executing tool '" + toolName + "': " + e.getMessage();
        }
    }

    public Collection<Tool> all() {
        log.debug("[TOOL_REGISTRY] Retrieving all {} available tools", toolsByName.size());
        return toolsByName.values();
    }
}
