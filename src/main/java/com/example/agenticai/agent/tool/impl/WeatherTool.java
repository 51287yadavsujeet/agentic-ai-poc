package com.example.agenticai.agent.tool.impl;

import com.example.agenticai.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Simulated weather lookup - stands in for a real external API call in this POC. */
@Component
public class WeatherTool implements Tool {

    private static final Map<String, String> MOCK_DATA = Map.of(
            "pune", "28C, partly cloudy",
            "mumbai", "31C, humid with light showers",
            "delhi", "35C, hazy",
            "bengaluru", "24C, light rain",
            "london", "17C, overcast",
            "new york", "22C, clear skies"
    );

    @Override
    public String name() {
        return "get_weather";
    }

    @Override
    public String description() {
        return "Returns the current weather for a given city. Data is simulated for this demo.";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "city", Map.of("type", "string", "description", "City name, e.g. Pune")
                ),
                "required", List.of("city")
        );
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        String city = String.valueOf(arguments.get("city")).toLowerCase(Locale.ROOT).trim();
        return MOCK_DATA.getOrDefault(city, "No weather data available for '" + city + "' (mock data source).");
    }
}
