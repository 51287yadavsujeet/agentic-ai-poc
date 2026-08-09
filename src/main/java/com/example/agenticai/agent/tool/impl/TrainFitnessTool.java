package com.example.agenticai.agent.tool.impl;

import com.example.agenticai.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class TrainFitnessTool implements Tool {

    @Override
    public String name() {
        return "train_fitness";
    }

    @Override
    public String description() {
        return "Provides basic workout training suggestions based on fitness goal, days per week, and experience level.";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "goal", Map.of("type", "string", "description", "Goal such as fat loss, muscle gain, stamina, mobility"),
                        "days_per_week", Map.of("type", "integer", "description", "Training days per week"),
                        "level", Map.of(
                                "type", "string",
                                "enum", List.of("beginner", "intermediate", "advanced"),
                                "description", "Training level"
                        )
                ),
                "required", List.of("goal", "days_per_week", "level")
        );
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        String goal = String.valueOf(arguments.get("goal")).trim().toLowerCase(Locale.ROOT);
        int daysPerWeek = toInt(arguments.get("days_per_week"));
        String level = String.valueOf(arguments.get("level")).trim().toLowerCase(Locale.ROOT);

        String plan = switch (goal) {
            case "fat loss" -> "mix strength training with steady cardio and 1 interval session";
            case "muscle gain" -> "prioritize progressive overload, compound lifts, and recovery";
            case "stamina" -> "combine zone-2 cardio, intervals, and recovery work";
            case "mobility" -> "focus on daily joint mobility, stretching, and controlled strength";
            default -> "balance strength, cardio, and mobility";
        };

        return """
                Fitness training help
                Goal: %s
                Days per week: %d
                Level: %s

                Suggested direction:
                - %s

                Weekly note:
                - Match volume to recovery and consistency rather than intensity spikes.
                """.formatted(goal, daysPerWeek, level, plan);
    }

    private int toInt(Object value) {
        return switch (value) {
            case Number n -> n.intValue();
            case null -> throw new IllegalArgumentException("Missing days_per_week argument");
            default -> Integer.parseInt(String.valueOf(value));
        };
    }
}
