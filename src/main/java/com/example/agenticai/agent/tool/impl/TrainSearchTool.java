package com.example.agenticai.agent.tool.impl;

import com.example.agenticai.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class TrainSearchTool implements Tool {

    @Override
    public String name() {
        return "search_trains";
    }

    @Override
    public String description() {
        return "Searches mock train options for a route and date, including train name, timing, and indicative fare. Useful for domestic travel planning.";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "origin", Map.of("type", "string", "description", "Departure city, e.g. Delhi"),
                        "destination", Map.of("type", "string", "description", "Arrival city, e.g. Agra"),
                        "departure_date", Map.of("type", "string", "description", "Travel date in YYYY-MM-DD format"),
                        "travel_class", Map.of(
                                "type", "string",
                                "enum", List.of("sleeper", "3A", "2A", "1A", "cc"),
                                "description", "Preferred train class"
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
        String travelClass = clean(arguments.getOrDefault("travel_class", "3A")).toLowerCase(Locale.ROOT);

        if (origin.isBlank() || destination.isBlank() || departureDate.isBlank()) {
            return "Origin, destination, and departure_date are required.";
        }

        String routeKey = (origin + "-" + destination).toLowerCase(Locale.ROOT);
        String options = switch (routeKey) {
            case "delhi-agra" -> """
                    1. Gatimaan Express, 08:10 -> 09:50, approx INR 750 in CC
                    2. Shatabdi Express, 06:00 -> 07:55, approx INR 720 in CC
                    3. Taj Express, 07:15 -> 10:05, approx INR 240 in Sleeper
                    """;
            case "mumbai-goa" -> """
                    1. Tejas Express, 05:50 -> 14:20, approx INR 1,850 in CC
                    2. Jan Shatabdi, 14:40 -> 23:30, approx INR 980 in 3A
                    3. Konkan Kanya Express, 23:05 -> 10:45, approx INR 620 in Sleeper
                    """;
            case "bangalore-mysore", "bengaluru-mysore" -> """
                    1. Vande Bharat Express, 06:00 -> 08:00, approx INR 1,100 in CC
                    2. Tippu Express, 11:30 -> 14:00, approx INR 180 in Sleeper
                    3. Chamundi Express, 18:00 -> 20:30, approx INR 210 in Sleeper
                    """;
            default -> """
                    1. Demo Express, 07:00 -> 11:30, approx INR 850
                    2. Sample Intercity, 13:20 -> 18:10, approx INR 640
                    3. Regional Fast, 21:10 -> 05:40, approx INR 540
                    """;
        };

        return """
                Train search for %s to %s
                Departure date: %s
                Preferred class: %s

                Available demo options:
                %s
                Note: These are mock train results for demo use only, not live availability.
                """.formatted(origin, destination, departureDate, travelClass, options.stripTrailing());
    }

    private String clean(Object value) {
        return String.valueOf(value == null ? "" : value).trim();
    }
}
