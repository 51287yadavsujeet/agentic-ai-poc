package com.example.agenticai.agent.tool.impl;

import com.example.agenticai.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class PackingHelpTool implements Tool {

    @Override
    public String name() {
        return "packing_help";
    }

    @Override
    public String description() {
        return "Creates a packing checklist based on destination, weather conditions, trip duration, and trip type. Useful for travel planning after checking weather.";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "destination", Map.of("type", "string", "description", "Destination city or region, e.g. Goa"),
                        "weather", Map.of("type", "string", "description", "Weather summary such as 35C, hazy or 24C, light rain"),
                        "days", Map.of("type", "integer", "description", "Trip duration in days"),
                        "trip_type", Map.of(
                                "type", "string",
                                "enum", List.of("leisure", "business", "adventure", "beach", "family"),
                                "description", "Primary type of trip"
                        )
                ),
                "required", List.of("destination", "weather", "days", "trip_type")
        );
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        String destination = String.valueOf(arguments.get("destination")).trim();
        String weather = String.valueOf(arguments.get("weather")).trim().toLowerCase(Locale.ROOT);
        int days = toInt(arguments.get("days"));
        String tripType = String.valueOf(arguments.get("trip_type")).trim().toLowerCase(Locale.ROOT);

        if (destination.isBlank()) {
            return "Destination is required.";
        }
        if (weather.isBlank()) {
            return "Weather is required.";
        }
        if (days <= 0) {
            return "Days must be greater than 0.";
        }

        List<String> items = new ArrayList<>();
        items.add("%d set(s) of daily wear".formatted(Math.max(days, 2)));
        items.add("toiletries and personal medication");
        items.add("phone charger and power bank");
        items.add("government ID and travel tickets");

        if (weather.contains("rain") || weather.contains("showers")) {
            items.add("umbrella or light rain jacket");
            items.add("water-resistant footwear");
        }
        if (weather.contains("hazy") || weather.contains("dust")) {
            items.add("mask or scarf for dust protection");
        }
        if (weather.contains("clear") || weather.contains("sun") || weather.contains("35c") || weather.contains("31c") || weather.contains("28c")) {
            items.add("sunscreen");
            items.add("cap or hat");
            items.add("sunglasses");
        }
        if (weather.contains("17c") || weather.contains("cold") || weather.contains("overcast")) {
            items.add("light jacket or layer");
        }

        switch (tripType) {
            case "business" -> {
                items.add("formal outfit");
                items.add("laptop and charger");
                items.add("notebook or business documents");
            }
            case "adventure" -> {
                items.add("sports shoes or trekking shoes");
                items.add("quick-dry clothes");
                items.add("small first-aid kit");
            }
            case "beach" -> {
                items.add("flip-flops");
                items.add("swimwear");
                items.add("light cotton clothes");
            }
            case "family" -> {
                items.add("kids' essentials if applicable");
                items.add("snacks and basic medicines");
            }
            default -> items.add("one flexible outfit for evening or local dining");
        }

        return """
                Packing checklist for %s
                Duration: %d day(s)
                Weather context: %s
                Trip type: %s

                Recommended items:
                - %s

                Note: This is a demo packing checklist generated from static rules. For best results, pair it with the get_weather tool first.
                """.formatted(destination, days, weather, tripType, String.join("\n- ", items));
    }

    private int toInt(Object value) {
        return switch (value) {
            case Number n -> n.intValue();
            case null -> throw new IllegalArgumentException("Missing days argument");
            default -> Integer.parseInt(String.valueOf(value));
        };
    }
}
