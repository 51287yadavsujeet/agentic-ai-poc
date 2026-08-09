package com.example.agenticai.agent.tool;

import com.example.agenticai.openai.model.FunctionDef;
import com.example.agenticai.openai.model.ToolDefinition;
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

    private final Map<String, Tool> toolsByName;

    public ToolRegistry(List<Tool> tools) {
        this.toolsByName = tools.stream().collect(Collectors.toMap(Tool::name, t -> t));
    }

    public List<ToolDefinition> toolDefinitions() {
        return toolsByName.values().stream()
                .map(t -> ToolDefinition.function(new FunctionDef(t.name(), t.description(), t.parameters())))
                .toList();
    }

    public String execute(String toolName, Map<String, Object> arguments) {
        Tool tool = toolsByName.get(toolName);
        if (tool == null) {
            return "Error: no tool registered with name '" + toolName + "'";
        }
        try {
            return tool.execute(arguments);
        } catch (Exception e) {
            return "Error executing tool '" + toolName + "': " + e.getMessage();
        }
    }

    public Collection<Tool> all() {
        return toolsByName.values();
    }
}
