package com.example.agenticai.agent.tool.impl;

import com.example.agenticai.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class MovieSuggestionsTool implements Tool {

    @Override
    public String name() {
        return "movie_suggestions";
    }

    @Override
    public String description() {
        return "Provides mock movie suggestions based on genre, language, and viewing preference.";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "genre", Map.of("type", "string", "description", "Genre such as action, comedy, thriller, family"),
                        "language", Map.of("type", "string", "description", "Preferred language"),
                        "preference", Map.of("type", "string", "description", "Preference such as light, intense, weekend, short")
                ),
                "required", List.of("genre", "language", "preference")
        );
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        String genre = String.valueOf(arguments.get("genre")).trim().toLowerCase(Locale.ROOT);
        String language = String.valueOf(arguments.get("language")).trim();
        String preference = String.valueOf(arguments.get("preference")).trim().toLowerCase(Locale.ROOT);

        String picks = switch (genre) {
            case "action" -> "1. Shadow Pursuit\n2. Final Drift\n3. Steel Run";
            case "comedy" -> "1. Weekend Chaos\n2. Laugh Route\n3. Flatmates Forever";
            case "thriller" -> "1. Silent Signal\n2. The Last Witness\n3. Dark Platform";
            case "family" -> "1. Summer Together\n2. The Little Promise\n3. Homebound Days";
            default -> "1. Open Frame\n2. City Stories\n3. Tomorrow's Turn";
        };

        return """
                Movie suggestions
                Genre: %s
                Language: %s
                Preference: %s

                Mock picks:
                %s
                """.formatted(genre, language, preference, picks);
    }
}
