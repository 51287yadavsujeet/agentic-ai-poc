package com.example.agenticai.agent.tool.impl;

import com.example.agenticai.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class BudgetTool implements Tool {

    @Override
    public String name() {
        return "estimate_trip_budget";
    }

    @Override
    public String description() {
        return "Estimates a total trip budget in INR based on destination, days, travelers, and budget style. Useful for high-level trip cost planning.";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "destination", Map.of("type", "string", "description", "Destination city or region, e.g. Goa"),
                        "days", Map.of("type", "integer", "description", "Number of trip days"),
                        "travelers", Map.of("type", "integer", "description", "Number of travelers"),
                        "budget", Map.of(
                                "type", "string",
                                "enum", List.of("budget", "mid_range", "luxury"),
                                "description", "Overall spending preference"
                        )
                ),
                "required", List.of("destination", "days", "travelers", "budget")
        );
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        String destination = String.valueOf(arguments.get("destination")).trim();
        int days = toInt(arguments.get("days"));
        int travelers = toInt(arguments.get("travelers"));
        String budget = String.valueOf(arguments.get("budget")).trim().toLowerCase(Locale.ROOT);

        if (destination.isBlank()) {
            return "Destination is required.";
        }
        if (days <= 0 || travelers <= 0) {
            return "Days and travelers must be greater than 0.";
        }

        int stayPerNight = switch (budget) {
            case "budget" -> 2500;
            case "mid_range" -> 6500;
            case "luxury" -> 16000;
            default -> throw new IllegalArgumentException("Budget must be one of: budget, mid_range, luxury");
        };
        int foodPerDayPerPerson = switch (budget) {
            case "budget" -> 900;
            case "mid_range" -> 1800;
            case "luxury" -> 4000;
            default -> 0;
        };
        int localTravelPerDay = switch (budget) {
            case "budget" -> 700;
            case "mid_range" -> 1600;
            case "luxury" -> 3500;
            default -> 0;
        };
        int sightseeingPerDayPerPerson = switch (budget) {
            case "budget" -> 600;
            case "mid_range" -> 1400;
            case "luxury" -> 3200;
            default -> 0;
        };

        int nights = Math.max(1, days - 1);
        int hotelTotal = stayPerNight * nights;
        int foodTotal = foodPerDayPerPerson * days * travelers;
        int localTravelTotal = localTravelPerDay * days;
        int sightseeingTotal = sightseeingPerDayPerPerson * days * travelers;
        int total = hotelTotal + foodTotal + localTravelTotal + sightseeingTotal;

        return """
                Budget estimate for %s
                Duration: %d day(s), %d traveler(s)
                Budget style: %s

                Estimated breakdown:
                - Hotel: INR %d
                - Food: INR %d
                - Local travel: INR %d
                - Sightseeing: INR %d

                Estimated total: INR %d
                Note: This is a demo estimate from static planning rules, not live pricing.
                """.formatted(destination, days, travelers, budget, hotelTotal, foodTotal, localTravelTotal, sightseeingTotal, total);
    }

    private int toInt(Object value) {
        return switch (value) {
            case Number n -> n.intValue();
            case null -> throw new IllegalArgumentException("Missing numeric argument");
            default -> Integer.parseInt(String.valueOf(value));
        };
    }
}
