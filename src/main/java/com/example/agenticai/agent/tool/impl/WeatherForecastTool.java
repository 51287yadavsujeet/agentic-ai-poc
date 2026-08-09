package com.example.agenticai.agent.tool.impl;

import com.example.agenticai.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class WeatherForecastTool implements Tool {

    @Override
    public String name() {
        return "weather_forecast";
    }

    @Override
    public String description() {
        return "Returns a short mock multi-day weather forecast for a city. Useful for packing and day planning.";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "city", Map.of("type", "string", "description", "City name, e.g. Goa"),
                        "days", Map.of("type", "integer", "description", "Number of forecast days, up to 5")
                ),
                "required", List.of("city", "days")
        );
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        String city = String.valueOf(arguments.get("city")).trim();
        int days = Math.min(Math.max(toInt(arguments.get("days")), 1), 5);
        String normalized = city.toLowerCase(Locale.ROOT);
        String[] pattern = switch (normalized) {
            case "goa" -> new String[]{"31C, humid with light showers", "30C, cloudy", "31C, sunny intervals", "29C, showers", "30C, humid"};
            case "delhi" -> new String[]{"35C, hazy", "36C, hot", "34C, partly cloudy", "35C, hazy", "33C, light wind"};
            case "bengaluru", "bangalore" -> new String[]{"24C, light rain", "25C, cloudy", "24C, breezy", "23C, showers", "24C, mild"};
            default -> new String[]{"28C, partly cloudy", "29C, sunny intervals", "27C, light showers", "28C, cloudy", "29C, clear"};
        };

        StringBuilder result = new StringBuilder("Forecast for ").append(city).append("\n");
        for (int i = 0; i < days; i++) {
            result.append("Day ").append(i + 1).append(": ").append(pattern[i]).append("\n");
        }
        result.append("Note: This is a mock forecast for demo use only.");
        return result.toString();
    }

    private int toInt(Object value) {
        return switch (value) {
            case Number n -> n.intValue();
            case null -> throw new IllegalArgumentException("Missing days argument");
            default -> Integer.parseInt(String.valueOf(value));
        };
    }
}
