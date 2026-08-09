package com.example.agenticai.agent.tool.impl;

import com.example.agenticai.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class CabFareTool implements Tool {

    @Override
    public String name() {
        return "estimate_cab_fare";
    }

    @Override
    public String description() {
        return "Estimates cab fare in INR for a local or intercity ride using static demo pricing. Useful for transfer and local transport planning.";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "pickup", Map.of("type", "string", "description", "Pickup location or city"),
                        "drop", Map.of("type", "string", "description", "Drop location or city"),
                        "distance_km", Map.of("type", "number", "description", "Approximate travel distance in kilometers"),
                        "cab_type", Map.of(
                                "type", "string",
                                "enum", List.of("mini", "sedan", "suv"),
                                "description", "Cab category"
                        )
                ),
                "required", List.of("pickup", "drop", "distance_km", "cab_type")
        );
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        String pickup = String.valueOf(arguments.get("pickup")).trim();
        String drop = String.valueOf(arguments.get("drop")).trim();
        double distanceKm = toDouble(arguments.get("distance_km"));
        String cabType = String.valueOf(arguments.get("cab_type")).trim().toLowerCase(Locale.ROOT);

        if (pickup.isBlank() || drop.isBlank()) {
            return "Pickup and drop are required.";
        }
        if (distanceKm <= 0) {
            return "distance_km must be greater than 0.";
        }

        int baseFare = switch (cabType) {
            case "mini" -> 80;
            case "sedan" -> 120;
            case "suv" -> 180;
            default -> throw new IllegalArgumentException("Cab type must be one of: mini, sedan, suv");
        };
        int perKm = switch (cabType) {
            case "mini" -> 14;
            case "sedan" -> 18;
            case "suv" -> 24;
            default -> 0;
        };

        long total = Math.round(baseFare + (distanceKm * perKm));

        return """
                Cab fare estimate
                Route: %s -> %s
                Distance: %.1f km
                Cab type: %s

                Estimated fare: INR %d
                Assumptions: base fare INR %d + INR %d/km
                Note: This is a demo fare estimate from static pricing, not a live cab quote.
                """.formatted(pickup, drop, distanceKm, cabType, total, baseFare, perKm);
    }

    private double toDouble(Object value) {
        return switch (value) {
            case Number n -> n.doubleValue();
            case null -> throw new IllegalArgumentException("Missing distance_km argument");
            default -> Double.parseDouble(String.valueOf(value));
        };
    }
}
