package com.example.agenticai.agent.tool.impl;

import com.example.agenticai.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Demo hotel lookup tool with deterministic sample properties.
 */
@Component
public class HotelDetailsTool implements Tool {

    @Override
    public String name() {
        return "get_hotel_details";
    }

    @Override
    public String description() {
        return "Returns mock hotel suggestions for a city, with nightly budget guidance and amenities. Useful for building a full trip plan.";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "city", Map.of("type", "string", "description", "City to search hotels in, e.g. Goa"),
                        "budget", Map.of(
                                "type", "string",
                                "enum", List.of("budget", "mid_range", "luxury"),
                                "description", "Hotel budget preference"
                        ),
                        "nights", Map.of("type", "integer", "description", "Number of nights"),
                        "guests", Map.of("type", "integer", "description", "Number of guests")
                ),
                "required", List.of("city", "budget")
        );
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        String city = clean(arguments.get("city"));
        String budget = clean(arguments.get("budget")).toLowerCase(Locale.ROOT);
        int nights = toInt(arguments.getOrDefault("nights", 1));
        int guests = toInt(arguments.getOrDefault("guests", 2));

        if (city.isBlank()) {
            return "City is required.";
        }

        String recommendations = switch ((city + "-" + budget).toLowerCase(Locale.ROOT)) {
            case "goa-budget" -> """
                    1. Beach Breeze Hostel - approx INR 1,800/night, free Wi-Fi, near Baga
                    2. Coastal Stay Inn - approx INR 2,400/night, breakfast included, scooter rental support
                    """;
            case "goa-mid_range" -> """
                    1. Sea View Residency - approx INR 5,800/night, pool, breakfast, Candolim area
                    2. Palm Grove Suites - approx INR 6,700/night, family rooms, airport pickup on request
                    """;
            case "goa-luxury" -> """
                    1. Azure Sands Resort - approx INR 14,500/night, private beach access, spa
                    2. Sunset Grand Goa - approx INR 18,900/night, premium dining, sea-facing suites
                    """;
            case "jaipur-budget" -> """
                    1. Pink City Lodge - approx INR 2,000/night, old city access, basic breakfast
                    2. Heritage Corner Stay - approx INR 2,650/night, rooftop cafe, walkable market area
                    """;
            case "jaipur-mid_range" -> """
                    1. Amber Courtyard Hotel - approx INR 5,200/night, pool, heritage decor
                    2. Raj Comfort Suites - approx INR 6,100/night, airport transfer, restaurant
                    """;
            case "jaipur-luxury" -> """
                    1. Royal Haveli Palace - approx INR 16,800/night, curated cultural dining, spa
                    2. Maharaja Grand Retreat - approx INR 21,500/night, premium suites, private chauffeur desk
                    """;
            default -> defaultRecommendations(budget);
        };

        return """
                Hotel suggestions for %s
                Budget: %s
                Nights: %d
                Guests: %d

                Available demo properties:
                %s
                Note: These are mock hotel recommendations for demo use only, not live availability.
                """.formatted(city, budget, nights, guests, recommendations.stripTrailing());
    }

    private String defaultRecommendations(String budget) {
        return switch (budget) {
            case "budget" -> """
                    1. City Budget Inn - approx INR 2,200/night, Wi-Fi, central access
                    2. Transit Comfort Stay - approx INR 2,900/night, breakfast, airport connectivity
                    """;
            case "mid_range" -> """
                    1. Urban Grand Hotel - approx INR 6,300/night, breakfast, business-friendly
                    2. Lakeview Residency - approx INR 7,100/night, restaurant, good local access
                    """;
            case "luxury" -> """
                    1. Signature Palace Hotel - approx INR 15,900/night, spa, concierge
                    2. Elite Horizon Suites - approx INR 19,800/night, premium dining, large rooms
                    """;
            default -> throw new IllegalArgumentException("Budget must be one of: budget, mid_range, luxury");
        };
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
