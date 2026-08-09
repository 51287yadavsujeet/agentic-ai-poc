package com.example.agenticai.agent.tool.impl;

import com.example.agenticai.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class PlacesToVisitTool implements Tool {

    @Override
    public String name() {
        return "places_to_visit";
    }

    @Override
    public String description() {
        return "Suggests places to visit based on city, trip type, and duration. Useful for itinerary construction.";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "city", Map.of("type", "string", "description", "City or destination, e.g. Goa"),
                        "trip_type", Map.of(
                                "type", "string",
                                "enum", List.of("leisure", "business", "adventure", "beach", "family"),
                                "description", "Primary trip type"
                        ),
                        "days", Map.of("type", "integer", "description", "Trip duration in days")
                ),
                "required", List.of("city", "trip_type", "days")
        );
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        String city = String.valueOf(arguments.get("city")).trim();
        String tripType = String.valueOf(arguments.get("trip_type")).trim().toLowerCase(Locale.ROOT);
        int days = toInt(arguments.get("days"));
        String normalized = city.toLowerCase(Locale.ROOT);

        String places = switch (normalized) {
            case "goa" -> """
                    - Baga / Candolim beach belt
                    - Fontainhas heritage walk
                    - Fort Aguada
                    - Sunset cruise or evening market
                    """;
            case "jaipur" -> """
                    - Amber Fort
                    - City Palace
                    - Hawa Mahal
                    - Johari Bazaar
                    """;
            case "delhi" -> """
                    - India Gate and central vista area
                    - Humayun's Tomb
                    - Qutub Minar
                    - Chandni Chowk food and market circuit
                    """;
            default -> """
                    - Main city landmark
                    - Old town or heritage quarter
                    - Popular food street
                    - One relaxed evening viewpoint or market
                    """;
        };

        String planHint = switch (tripType) {
            case "business" -> "Keep sightseeing concentrated in one half-day and one evening.";
            case "adventure" -> "Add one outdoor or activity-heavy segment each day.";
            case "beach" -> "Keep mornings or sunsets for scenic spots and afternoons flexible.";
            case "family" -> "Mix major attractions with low-movement breaks.";
            default -> "Balance landmarks, food, and one flexible block.";
        };

        return """
                Places to visit for %s
                Trip type: %s
                Duration: %d day(s)

                Suggestions:
                %s

                Planning hint:
                %s
                """.formatted(city, tripType, days, places.stripTrailing(), planHint);
    }

    private int toInt(Object value) {
        return switch (value) {
            case Number n -> n.intValue();
            case null -> throw new IllegalArgumentException("Missing days argument");
            default -> Integer.parseInt(String.valueOf(value));
        };
    }
}
