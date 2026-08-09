package com.example.agenticai.agent.tool.impl;

import com.example.agenticai.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class BadmintonHelpTool implements Tool {

    @Override
    public String name() {
        return "badminton_help";
    }

    @Override
    public String description() {
        return "Provides badminton practice, equipment, and fitness suggestions based on player level and goal.";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "level", Map.of(
                                "type", "string",
                                "enum", List.of("beginner", "intermediate", "advanced"),
                                "description", "Player level"
                        ),
                        "goal", Map.of("type", "string", "description", "Goal such as fitness, footwork, smash, doubles, stamina")
                ),
                "required", List.of("level", "goal")
        );
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        String level = String.valueOf(arguments.get("level")).trim().toLowerCase(Locale.ROOT);
        String goal = String.valueOf(arguments.get("goal")).trim().toLowerCase(Locale.ROOT);

        String drills = switch (level) {
            case "beginner" -> "shadow swings, basic serves, short rallies, front-back movement";
            case "intermediate" -> "multi-shuttle drills, cross-court clears, net kills, recovery footwork";
            case "advanced" -> "high-intensity multi-feed, deception drills, jump smash sequence, match simulation";
            default -> throw new IllegalArgumentException("Level must be beginner, intermediate, or advanced");
        };

        String focus = switch (goal) {
            case "fitness" -> "Add skipping, lunges, and interval court sprints.";
            case "footwork" -> "Prioritize split-step timing and six-corner movement drills.";
            case "smash" -> "Focus on shoulder rotation, jump timing, and follow-through.";
            case "doubles" -> "Work on rotation, drive exchanges, and serve-return patterns.";
            case "stamina" -> "Increase rally duration and add interval conditioning.";
            default -> "Balance technique, footwork, and match play.";
        };

        return """
                Badminton help
                Level: %s
                Goal: %s

                Suggested drills:
                - %s

                Focus note:
                %s
                """.formatted(level, goal, drills, focus);
    }
}
