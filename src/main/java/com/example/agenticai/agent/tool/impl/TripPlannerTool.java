package com.example.agenticai.agent.tool.impl;

import com.example.agenticai.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Simple deterministic trip-planning tool for the demo.
 * It generates a compact itinerary from structured inputs rather than relying on live travel APIs.
 */
@Component
public class TripPlannerTool implements Tool {

    @Override
    public String name() {
        return "plan_trip";
    }

    @Override
    public String description() {
        return "Creates a short trip plan with destination, trip duration, budget style, and suggested activities. Uses static planning logic for this demo.";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "destination", Map.of("type", "string", "description", "Destination city or region, e.g. Goa or Jaipur"),
                        "days", Map.of("type", "integer", "description", "Number of trip days, e.g. 3"),
                        "budget", Map.of(
                                "type", "string",
                                "enum", List.of("budget", "mid_range", "luxury"),
                                "description", "Travel budget category"
                        ),
                        "interests", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string"),
                                "description", "Traveler interests such as food, history, nature, shopping, nightlife"
                        )
                ),
                "required", List.of("destination", "days", "budget")
        );
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        String destination = String.valueOf(arguments.get("destination")).trim();
        int days = toInt(arguments.get("days"));
        String budget = String.valueOf(arguments.get("budget")).trim();
        String interests = formatInterests(arguments.get("interests"));

        if (destination.isBlank()) {
            return "Destination is required.";
        }
        if (days <= 0) {
            return "Days must be greater than 0.";
        }

        return """
                Trip plan for %s
                Duration: %d day(s)
                Budget: %s
                Interests: %s

                Suggested itinerary:
                Day 1: Arrival, local orientation, and one signature attraction.
                Day 2: Core sightseeing plus a food or culture experience.
                Day 3: Flexible half-day for shopping, relaxation, or a nearby excursion.

                Budget guidance:
                - budget: hostels, public transport, local eateries
                - mid_range: 3-star stay, cabs for selected travel, mixed dining
                - luxury: premium hotel, private transport, curated experiences

                Recommended focus for %s: %s
                Note: This is a demo trip plan generated from static rules, not live booking or weather data.
                """.formatted(destination, days, budget, interests, destination, recommendationFor(interests));
    }

    private int toInt(Object value) {
        return switch (value) {
            case Number n -> n.intValue();
            case null -> throw new IllegalArgumentException("Missing days argument");
            default -> Integer.parseInt(String.valueOf(value));
        };
    }

    private String formatInterests(Object interestsValue) {
        if (interestsValue == null) {
            return "general sightseeing";
        }
        if (interestsValue instanceof List<?> list) {
            List<String> values = list.stream()
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList();
            return values.isEmpty() ? "general sightseeing" : String.join(", ", values);
        }
        String value = String.valueOf(interestsValue).trim();
        return value.isBlank() ? "general sightseeing" : value;
    }

    private String recommendationFor(String interests) {
        String normalized = interests.toLowerCase();
        if (normalized.contains("food")) return "prioritize local markets and well-known regional restaurants";
        if (normalized.contains("history")) return "focus on heritage sites, museums, and guided walks";
        if (normalized.contains("nature")) return "plan early outdoor sessions and scenic viewpoints";
        if (normalized.contains("shopping")) return "reserve evening time for local bazaars and retail districts";
        if (normalized.contains("nightlife")) return "keep the daytime lighter and concentrate activities in the evening";
        return "balance landmarks, food, and one unstructured exploration window";
    }
}
