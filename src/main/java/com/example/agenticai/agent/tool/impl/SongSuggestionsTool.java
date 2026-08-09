package com.example.agenticai.agent.tool.impl;

import com.example.agenticai.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class SongSuggestionsTool implements Tool {

    @Override
    public String name() {
        return "song_suggestions";
    }

    @Override
    public String description() {
        return "Provides mock song suggestions based on mood, language, and listening context.";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "mood", Map.of("type", "string", "description", "Mood such as chill, romantic, energetic, focus"),
                        "language", Map.of("type", "string", "description", "Preferred language such as English, Hindi"),
                        "context", Map.of("type", "string", "description", "Listening context such as workout, drive, study, evening")
                ),
                "required", List.of("mood", "language", "context")
        );
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        String mood = String.valueOf(arguments.get("mood")).trim().toLowerCase(Locale.ROOT);
        String language = String.valueOf(arguments.get("language")).trim();
        String context = String.valueOf(arguments.get("context")).trim().toLowerCase(Locale.ROOT);

        String list = switch (mood) {
            case "chill" -> "1. Midnight Breeze\n2. Soft Horizon\n3. City Lights Acoustic";
            case "romantic" -> "1. Heartline\n2. Moonlit Letter\n3. Silent Promise";
            case "energetic" -> "1. Pulse Run\n2. Rise Faster\n3. Ignite Tonight";
            case "focus" -> "1. Quiet Motion\n2. Deep Work Flow\n3. Minimal Drift";
            default -> "1. Everyday Echo\n2. Open Road\n3. Weekend Replay";
        };

        return """
                Song suggestions
                Mood: %s
                Language: %s
                Context: %s

                Mock playlist:
                %s
                """.formatted(mood, language, context, list);
    }
}
