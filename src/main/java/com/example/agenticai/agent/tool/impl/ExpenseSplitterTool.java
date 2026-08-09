package com.example.agenticai.agent.tool.impl;

import com.example.agenticai.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Component
public class ExpenseSplitterTool implements Tool {

    @Override
    public String name() {
        return "expense_splitter";
    }

    @Override
    public String description() {
        return "Splits a total trip expense across a group and optionally adds a contingency percentage.";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "total_amount", Map.of("type", "number", "description", "Total expense amount"),
                        "people", Map.of("type", "integer", "description", "Number of people sharing the expense"),
                        "contingency_percent", Map.of("type", "number", "description", "Optional contingency percentage, e.g. 10")
                ),
                "required", List.of("total_amount", "people")
        );
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        BigDecimal total = toBigDecimal(arguments.get("total_amount"));
        int people = toInt(arguments.get("people"));
        BigDecimal contingencyPercent = arguments.get("contingency_percent") == null
                ? BigDecimal.ZERO
                : toBigDecimal(arguments.get("contingency_percent"));

        if (people <= 0) {
            return "People must be greater than 0.";
        }

        BigDecimal contingency = total.multiply(contingencyPercent).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal finalTotal = total.add(contingency);
        BigDecimal perPerson = finalTotal.divide(BigDecimal.valueOf(people), 2, RoundingMode.HALF_UP);

        return """
                Expense split
                Base total: %.2f
                Contingency: %.2f
                Final total: %.2f
                People: %d
                Per person: %.2f
                """.formatted(total, contingency, finalTotal, people, perPerson);
    }

    private BigDecimal toBigDecimal(Object value) {
        return switch (value) {
            case Number n -> BigDecimal.valueOf(n.doubleValue());
            case null -> throw new IllegalArgumentException("Missing numeric argument");
            default -> new BigDecimal(String.valueOf(value));
        };
    }

    private int toInt(Object value) {
        return switch (value) {
            case Number n -> n.intValue();
            case null -> throw new IllegalArgumentException("Missing people argument");
            default -> Integer.parseInt(String.valueOf(value));
        };
    }
}
