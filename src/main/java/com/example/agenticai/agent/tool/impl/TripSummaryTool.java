package com.example.agenticai.agent.tool.impl;

import com.example.agenticai.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TripSummaryTool implements Tool {

    @Override
    public String name() {
        return "trip_summary";
    }

    @Override
    public String description() {
        return "Combines itinerary, transport, hotel, budget, packing, and medical notes into one structured trip summary.";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "destination", Map.of("type", "string", "description", "Destination"),
                        "itinerary", Map.of("type", "string", "description", "Itinerary summary"),
                        "transport", Map.of("type", "string", "description", "Flight/train/cab summary"),
                        "hotel", Map.of("type", "string", "description", "Hotel summary"),
                        "budget", Map.of("type", "string", "description", "Budget summary"),
                        "packing", Map.of("type", "string", "description", "Packing summary"),
                        "medical", Map.of("type", "string", "description", "Medical summary")
                ),
                "required", List.of("destination")
        );
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        String destination = get(arguments, "destination");
        return """
                Full trip summary for %s

                Itinerary:
                %s

                Transport:
                %s

                Hotel:
                %s

                Budget:
                %s

                Packing:
                %s

                Medical:
                %s
                """.formatted(
                destination,
                getOrDefault(arguments, "itinerary", "Not provided"),
                getOrDefault(arguments, "transport", "Not provided"),
                getOrDefault(arguments, "hotel", "Not provided"),
                getOrDefault(arguments, "budget", "Not provided"),
                getOrDefault(arguments, "packing", "Not provided"),
                getOrDefault(arguments, "medical", "Not provided")
        );
    }

    private String get(Map<String, Object> arguments, String key) {
        String value = getOrDefault(arguments, key, "");
        if (value.isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value;
    }

    private String getOrDefault(Map<String, Object> arguments, String key, String fallback) {
        Object value = arguments.get(key);
        return value == null ? fallback : String.valueOf(value);
    }
}
