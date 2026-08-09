package com.example.agenticai.agent.tool.impl;

import com.example.agenticai.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class DrivingHelpTool implements Tool {

    @Override
    public String name() {
        return "driving_help";
    }

    @Override
    public String description() {
        return "Provides basic driving practice suggestions and road-trip preparation advice based on driver experience and scenario.";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "experience", Map.of(
                                "type", "string",
                                "enum", List.of("learner", "regular", "experienced"),
                                "description", "Driver experience level"
                        ),
                        "scenario", Map.of("type", "string", "description", "Scenario such as city, highway, parking, rain, night")
                ),
                "required", List.of("experience", "scenario")
        );
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        String experience = String.valueOf(arguments.get("experience")).trim().toLowerCase(Locale.ROOT);
        String scenario = String.valueOf(arguments.get("scenario")).trim().toLowerCase(Locale.ROOT);

        String checklist = switch (scenario) {
            case "city" -> "mirror checks, lane discipline, gap judgement, slow-speed control";
            case "highway" -> "merge planning, lane awareness, speed discipline, rest stops";
            case "parking" -> "mirror alignment, steering correction, reference-point control";
            case "rain" -> "lower speed, higher braking distance, wiper and tire check";
            case "night" -> "headlight discipline, visibility focus, fatigue management";
            default -> "vehicle check, calm steering input, hazard awareness";
        };

        String guidance = switch (experience) {
            case "learner" -> "Practice in low-traffic conditions and avoid high-complexity roads initially.";
            case "regular" -> "Work on consistency, anticipation, and smoother braking/acceleration.";
            case "experienced" -> "Focus on defensive driving and long-distance fatigue management.";
            default -> throw new IllegalArgumentException("Experience must be learner, regular, or experienced");
        };

        return """
                Driving help
                Experience: %s
                Scenario: %s

                Practice checklist:
                - %s

                Guidance:
                %s
                """.formatted(experience, scenario, checklist, guidance);
    }
}
