package com.example.agenticai.agent.tool.impl;

import com.example.agenticai.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class BookSuggestionsTool implements Tool {

    @Override
    public String name() {
        return "book_suggestions";
    }

    @Override
    public String description() {
        return "Provides mock book suggestions based on genre, reading goal, and difficulty preference.";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "genre", Map.of("type", "string", "description", "Genre such as fiction, self-help, business, mystery"),
                        "reading_goal", Map.of("type", "string", "description", "Goal such as learning, relaxation, habit, inspiration"),
                        "difficulty", Map.of(
                                "type", "string",
                                "enum", List.of("easy", "medium", "advanced"),
                                "description", "Reading difficulty preference"
                        )
                ),
                "required", List.of("genre", "reading_goal", "difficulty")
        );
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        String genre = String.valueOf(arguments.get("genre")).trim().toLowerCase(Locale.ROOT);
        String readingGoal = String.valueOf(arguments.get("reading_goal")).trim().toLowerCase(Locale.ROOT);
        String difficulty = String.valueOf(arguments.get("difficulty")).trim().toLowerCase(Locale.ROOT);

        String books = switch (genre) {
            case "fiction" -> "1. The Quiet Mile\n2. Rain over Harbor Street\n3. A Room of Echoes";
            case "self-help" -> "1. Better by Design\n2. Small Daily Wins\n3. Focus without Noise";
            case "business" -> "1. Systems that Scale\n2. The Practical Operator\n3. Strategy under Constraint";
            case "mystery" -> "1. The Tenth Platform\n2. Silent Ledger\n3. Before Midnight Falls";
            default -> "1. Open Chapters\n2. Clear Thinking Notes\n3. The Long Weekend Read";
        };

        return """
                Book suggestions
                Genre: %s
                Reading goal: %s
                Difficulty: %s

                Mock picks:
                %s
                """.formatted(genre, readingGoal, difficulty, books);
    }
}
