package com.example.agenticai.agent.tool.impl;

import com.example.agenticai.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** Basic arithmetic tool - deterministic, so it's a good way to prove the model is really
 *  delegating instead of guessing math on its own. */
@Component
public class CalculatorTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(CalculatorTool.class);

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
        log.info("[CALCULATOR_TOOL] Executing calculator tool with arguments: {}", arguments);
        
        try {
            double a = toDouble(arguments.get("a"));
            double b = toDouble(arguments.get("b"));
            String operation = String.valueOf(arguments.get("operation"));

            log.info("[CALCULATOR_TOOL] Calculation Details | operation='{}' | a={} | b={}",
                    operation, a, b);

            // JDK 21 pattern-matching switch expression
            double result = switch (operation) {
                case "add" -> {
                    log.debug("[CALCULATOR_TOOL] Performing addition: {} + {}", a, b);
                    yield a + b;
                }
                case "subtract" -> {
                    log.debug("[CALCULATOR_TOOL] Performing subtraction: {} - {}", a, b);
                    yield a - b;
                }
                case "multiply" -> {
                    log.debug("[CALCULATOR_TOOL] Performing multiplication: {} * {}", a, b);
                    yield a * b;
                }
                case "divide" -> {
                    if (b == 0) {
                        log.error("[CALCULATOR_TOOL] Division by zero error: {} / {}", a, b);
                        throw new ArithmeticException("Division by zero");
                    }
                    log.debug("[CALCULATOR_TOOL] Performing division: {} / {}", a, b);
                    yield a / b;
                }
                default -> {
                    log.error("[CALCULATOR_TOOL] Unknown operation: {}", operation);
                    throw new IllegalArgumentException("Unknown operation: " + operation);
                }
            };

            String resultStr = String.valueOf(result);
            log.info("[CALCULATOR_TOOL] Calculation Result | operation='{}' | result={}",
                    operation, resultStr);
            
            return resultStr;
        } catch (Exception e) {
            log.error("[CALCULATOR_TOOL] Tool execution error: {}", e.getMessage(), e);
            throw e;
        }
    }

    private double toDouble(Object value) {
        return switch (value) {
            case Number n -> n.doubleValue();
            case null -> throw new IllegalArgumentException("Missing numeric argument");
            default -> Double.parseDouble(String.valueOf(value));
        };
    }
}
