package com.example.agenticai.agent.tool.impl;

import com.example.agenticai.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class LocalTransportHelpTool implements Tool {

    @Override
    public String name() {
        return "local_transport_help";
    }

    @Override
    public String description() {
        return "Suggests local transport options in a city such as metro, bus, cab, airport transfer, scooter rental, or walkable zones. Uses static demo guidance.";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "city", Map.of("type", "string", "description", "City name, e.g. Delhi"),
                        "trip_type", Map.of(
                                "type", "string",
                                "enum", List.of("leisure", "business", "adventure", "beach", "family"),
                                "description", "Primary trip type"
                        )
                ),
                "required", List.of("city", "trip_type")
        );
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        String city = String.valueOf(arguments.get("city")).trim();
        String tripType = String.valueOf(arguments.get("trip_type")).trim().toLowerCase(Locale.ROOT);
        String normalizedCity = city.toLowerCase(Locale.ROOT);

        String options = switch (normalizedCity) {
            case "delhi" -> """
                    - Metro for fast city travel
                    - App cabs for airport and door-to-door trips
                    - Auto-rickshaws for short distances
                    - Airport Express for efficient airport transfer
                    """;
            case "mumbai" -> """
                    - Local trains for major north-south travel
                    - Metro on supported corridors
                    - App cabs for comfort and late hours
                    - Kaali-peeli taxis for short city trips
                    """;
            case "goa" -> """
                    - Scooter rental for flexible beach travel
                    - App or local cabs for airport and late-night travel
                    - Private transfers for group travel
                    - Walk in compact beach-market areas
                    """;
            case "bangalore", "bengaluru" -> """
                    - Metro where available
                    - App cabs for business and airport travel
                    - Auto-rickshaws for short distances
                    - Rental bike for quick solo travel
                    """;
            default -> """
                    - App cabs for convenience
                    - Local bus or metro if available
                    - Walk for short tourist circuits
                    - Private transfer for early/late travel
                    """;
        };

        String tripHint = switch (tripType) {
            case "business" -> "Prioritize airport transfer, predictable travel time, and app cabs.";
            case "adventure" -> "Use flexible mobility such as rental bikes or cabs for offbeat spots.";
            case "beach" -> "Scooter rental and short local cabs usually work best.";
            case "family" -> "Prefer safe point-to-point cabs and reduce multiple mode changes.";
            default -> "Mix public transport with cabs depending on distance and schedule.";
        };

        return """
                Local transport help for %s
                Trip type: %s

                Suggested options:
                %s

                Recommendation:
                %s
                """.formatted(city, tripType, options.stripTrailing(), tripHint);
    }
}
