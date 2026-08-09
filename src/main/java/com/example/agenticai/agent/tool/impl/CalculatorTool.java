package com.example.agenticai.agent.tool.impl;

import com.example.agenticai.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** Basic arithmetic tool - deterministic, so it's a good way to prove the model is really
 *  delegating instead of guessing math on its own. */
@Component
public class CalculatorTool implements Tool {

    @Override
    public String name() {
        return "calculator";
    }

    @Override
    public String description() {
        return "Performs a basic arithmetic operation (add, subtract, multiply, divide) on two numbers.";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "a", Map.of("type", "number", "description", "First operand"),
                        "b", Map.of("type", "number", "description", "Second operand"),
                        "operation", Map.of(
                                "type", "string",
                                "enum", List.of("add", "subtract", "multiply", "divide"),
                                "description", "Arithmetic operation to perform"
                        )
                ),
                "required", List.of("a", "b", "operation")
        );
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        double a = toDouble(arguments.get("a"));
        double b = toDouble(arguments.get("b"));
        String operation = String.valueOf(arguments.get("operation"));

        // JDK 21 pattern-matching switch expression
        double result = switch (operation) {
            case "add" -> a + b;
            case "subtract" -> a - b;
            case "multiply" -> a * b;
            case "divide" -> {
                if (b == 0) {
                    throw new ArithmeticException("Division by zero");
                }
                yield a / b;
            }
            default -> throw new IllegalArgumentException("Unknown operation: " + operation);
        };

        return String.valueOf(result);
    }

    private double toDouble(Object value) {
        return switch (value) {
            case Number n -> n.doubleValue();
            case null -> throw new IllegalArgumentException("Missing numeric argument");
            default -> Double.parseDouble(String.valueOf(value));
        };
    }
}
