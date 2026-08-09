package com.example.agenticai.agent.tool.impl;

import com.example.agenticai.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Demo currency conversion tool.
 * Uses a static USD->INR exchange rate so the agent has a deterministic budgeting utility.
 */
@Component
public class CurrencyConversionTool implements Tool {

    private static final BigDecimal USD_TO_INR = new BigDecimal("83.25");

    @Override
    public String name() {
        return "calculate_currency";
    }

    @Override
    public String description() {
        return "Converts currency amounts between USD and INR using a static demo exchange rate. Best for quick travel budget estimation.";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "amount", Map.of("type", "number", "description", "Amount to convert"),
                        "from_currency", Map.of(
                                "type", "string",
                                "enum", List.of("USD", "INR"),
                                "description", "Source currency"
                        ),
                        "to_currency", Map.of(
                                "type", "string",
                                "enum", List.of("USD", "INR"),
                                "description", "Target currency"
                        )
                ),
                "required", List.of("amount", "from_currency", "to_currency")
        );
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        BigDecimal amount = toBigDecimal(arguments.get("amount"));
        String fromCurrency = String.valueOf(arguments.get("from_currency")).toUpperCase(Locale.ROOT).trim();
        String toCurrency = String.valueOf(arguments.get("to_currency")).toUpperCase(Locale.ROOT).trim();

        if (fromCurrency.equals(toCurrency)) {
            return "%s %.2f equals %s %.2f (same currency).".formatted(fromCurrency, amount, toCurrency, amount);
        }

        BigDecimal converted = switch (fromCurrency + "_" + toCurrency) {
            case "USD_INR" -> amount.multiply(USD_TO_INR);
            case "INR_USD" -> amount.divide(USD_TO_INR, 2, RoundingMode.HALF_UP);
            default -> throw new IllegalArgumentException("Supported conversions are only between USD and INR.");
        };

        converted = converted.setScale(2, RoundingMode.HALF_UP);
        return "%s %.2f = %s %.2f using static demo rate 1 USD = INR %s"
                .formatted(fromCurrency, amount, toCurrency, converted, USD_TO_INR);
    }

    private BigDecimal toBigDecimal(Object value) {
        return switch (value) {
            case Number n -> BigDecimal.valueOf(n.doubleValue());
            case null -> throw new IllegalArgumentException("Missing amount argument");
            default -> new BigDecimal(String.valueOf(value));
        };
    }
}
