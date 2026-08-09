package com.example.agenticai.agent.tool.impl;

import com.example.agenticai.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Demo flight-search tool with deterministic mock recommendations.
 */
@Component
public class FlightSearchTool implements Tool {

    @Override
    public String name() {
        return "search_flights";
    }

    @Override
    public String description() {
        return "Searches mock flight options for a route and date, including indicative airline, timing, and fare. Useful for trip-planning demos.";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "origin", Map.of("type", "string", "description", "Departure city, e.g. Delhi"),
                        "destination", Map.of("type", "string", "description", "Arrival city, e.g. Goa"),
                        "departure_date", Map.of("type", "string", "description", "Travel date in YYYY-MM-DD format"),
                        "travelers", Map.of("type", "integer", "description", "Number of travelers"),
                        "cabin_class", Map.of(
                                "type", "string",
                                "enum", List.of("economy", "premium_economy", "business"),
                                "description", "Preferred cabin class"
                        )
                ),
                "required", List.of("origin", "destination", "departure_date")
        );
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        String origin = clean(arguments.get("origin"));
        String destination = clean(arguments.get("destination"));
        String departureDate = clean(arguments.get("departure_date"));
        int travelers = toInt(arguments.getOrDefault("travelers", 1));
        String cabinClass = clean(arguments.getOrDefault("cabin_class", "economy")).toLowerCase(Locale.ROOT);

        if (origin.isBlank() || destination.isBlank() || departureDate.isBlank()) {
            return "Origin, destination, and departure_date are required.";
        }

        String routeKey = (origin + "-" + destination).toLowerCase(Locale.ROOT);
        String options = switch (routeKey) {
            case "delhi-goa" -> """
                    1. IndiGo 6E-245, 08:10 -> 10:45, non-stop, approx INR 6,800 per traveler
                    2. Air India AI-883, 13:20 -> 16:05, non-stop, approx INR 7,450 per traveler
                    3. Vistara UK-947, 18:40 -> 21:20, non-stop, approx INR 8,100 per traveler
                    """;
            case "mumbai-goa" -> """
                    1. Akasa Air QP-1712, 09:15 -> 10:35, non-stop, approx INR 4,900 per traveler
                    2. IndiGo 6E-5123, 14:05 -> 15:25, non-stop, approx INR 5,350 per traveler
                    3. Air India AI-649, 19:30 -> 20:55, non-stop, approx INR 5,900 per traveler
                    """;
            case "bangalore-jaipur", "bengaluru-jaipur" -> """
                    1. IndiGo 6E-701, 06:45 -> 09:30, non-stop, approx INR 7,200 per traveler
                    2. Air India AI-509, 12:10 -> 15:20, 1 stop, approx INR 8,050 per traveler
                    3. Vistara UK-814, 17:50 -> 20:40, non-stop, approx INR 8,600 per traveler
                    """;
            default -> """
                    1. DemoAir DA-101, 08:00 -> 10:30, approx INR 6,500 per traveler
                    2. DemoJet DJ-202, 13:00 -> 15:40, approx INR 7,100 per traveler
                    3. SampleWings SW-303, 19:00 -> 21:50, approx INR 7,850 per traveler
                    """;
        };

        return """
                Flight search for %s to %s
                Departure date: %s
                Travelers: %d
                Cabin class: %s

                Available demo options:
                %s
                Note: These are mock flight results for demo use only, not live inventory.
                """.formatted(origin, destination, departureDate, travelers, cabinClass, options.stripTrailing());
    }

    private String clean(Object value) {
        return String.valueOf(value == null ? "" : value).trim();
    }

    private int toInt(Object value) {
        return switch (value) {
            case Number n -> n.intValue();
            case null -> 1;
            default -> Integer.parseInt(String.valueOf(value));
        };
    }
}
