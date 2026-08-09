package com.example.agenticai.agent.tool.impl;

import com.example.agenticai.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class RestaurantSuggestionsTool implements Tool {

    @Override
    public String name() {
        return "restaurant_suggestions";
    }

    @Override
    public String description() {
        return "Provides mock restaurant suggestions by city, cuisine preference, and budget. Useful for itinerary and food-focused travel planning.";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "city", Map.of("type", "string", "description", "City name, e.g. Goa"),
                        "cuisine", Map.of("type", "string", "description", "Cuisine preference, e.g. seafood, north indian, italian"),
                        "budget", Map.of(
                                "type", "string",
                                "enum", List.of("budget", "mid_range", "luxury"),
                                "description", "Meal budget preference"
                        )
                ),
                "required", List.of("city", "cuisine", "budget")
        );
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        String city = String.valueOf(arguments.get("city")).trim();
        String cuisine = String.valueOf(arguments.get("cuisine")).trim().toLowerCase(Locale.ROOT);
        String budget = String.valueOf(arguments.get("budget")).trim().toLowerCase(Locale.ROOT);
        String key = (city + "-" + cuisine + "-" + budget).toLowerCase(Locale.ROOT);

        String suggestions = switch (key) {
            case "goa-seafood-budget" -> """
                    1. Fisherman's Corner Local - approx INR 600 for two
                    2. Coastal Catch Shack - approx INR 750 for two
                    """;
            case "goa-seafood-mid_range" -> """
                    1. Spice Tide Bistro - approx INR 1,600 for two
                    2. Blue Harbor Grill - approx INR 1,900 for two
                    """;
            case "jaipur-rajasthani-mid_range" -> """
                    1. Raj Thali House - approx INR 1,200 for two
                    2. Pink Courtyard Dining - approx INR 1,650 for two
                    """;
            case "delhi-north indian-mid_range" -> """
                    1. Dilli Tandoor Room - approx INR 1,500 for two
                    2. Capital Curry Table - approx INR 1,850 for two
                    """;
            default -> """
                    1. City Flavor Kitchen - mock recommendation
                    2. Urban Taste House - mock recommendation
                    3. Local Plate Studio - mock recommendation
                    """;
        };

        return """
                Restaurant suggestions
                City: %s
                Cuisine: %s
                Budget: %s

                Options:
                %s

                Note: These are mock recommendations for demo use only.
                """.formatted(city, cuisine, budget, suggestions.stripTrailing());
    }
}
