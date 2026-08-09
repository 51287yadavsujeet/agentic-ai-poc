package com.example.agenticai.agent.tool.impl;

import com.example.agenticai.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class LanguageHelpTool implements Tool {

    @Override
    public String name() {
        return "language_help";
    }

    @Override
    public String description() {
        return "Provides a few useful travel phrases for a selected local language, such as greeting, help, and transport phrases.";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "language", Map.of("type", "string", "description", "Language name, e.g. Hindi, Marathi, Thai"),
                        "context", Map.of(
                                "type", "string",
                                "enum", List.of("general", "transport", "restaurant", "emergency"),
                                "description", "Phrase context"
                        )
                ),
                "required", List.of("language", "context")
        );
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        String language = String.valueOf(arguments.get("language")).trim();
        String context = String.valueOf(arguments.get("context")).trim().toLowerCase(Locale.ROOT);
        String key = language.toLowerCase(Locale.ROOT);

        String phrases = switch (key) {
            case "hindi" -> phrasesForHindi(context);
            case "marathi" -> phrasesForMarathi(context);
            default -> """
                    - Hello
                    - Please help me
                    - How much does this cost?
                    - Thank you
                    """;
        };

        return """
                Language help
                Language: %s
                Context: %s

                Useful phrases:
                %s
                """.formatted(language, context, phrases.stripTrailing());
    }

    private String phrasesForHindi(String context) {
        return switch (context) {
            case "transport" -> """
                    - Station kahan hai? (Where is the station?)
                    - Mujhe yahan jaana hai. (I want to go here.)
                    - Kitna kiraya hoga? (What will the fare be?)
                    """;
            case "restaurant" -> """
                    - Menu dikhaiye. (Please show the menu.)
                    - Ismein kya hai? (What is in this?)
                    - Bill dijiye. (Please give the bill.)
                    """;
            case "emergency" -> """
                    - Madad chahiye. (I need help.)
                    - Doctor kahan milega? (Where can I find a doctor?)
                    - Police ko bulaaiye. (Call the police.)
                    """;
            default -> """
                    - Namaste (Hello)
                    - Dhanyavaad (Thank you)
                    - Kripya madad kijiye. (Please help.)
                    """;
        };
    }

    private String phrasesForMarathi(String context) {
        return switch (context) {
            case "transport" -> """
                    - Station kuthe aahe? (Where is the station?)
                    - Mala ithe jaycha aahe. (I want to go here.)
                    - Bhada kiti? (How much is the fare?)
                    """;
            case "restaurant" -> """
                    - Menu dakhva. (Show the menu.)
                    - Ya madhye kay aahe? (What is in this?)
                    - Bill dya. (Please give the bill.)
                    """;
            case "emergency" -> """
                    - Mala madat pahije. (I need help.)
                    - Doctor kuthe aahet? (Where is the doctor?)
                    - Police la bola. (Call the police.)
                    """;
            default -> """
                    - Namaskar (Hello)
                    - Dhanyavaad (Thank you)
                    - Krupaya madat kara. (Please help.)
                    """;
        };
    }
}
