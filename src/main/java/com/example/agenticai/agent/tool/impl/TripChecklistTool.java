package com.example.agenticai.agent.tool.impl;

import com.example.agenticai.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class TripChecklistTool implements Tool {

    @Override
    public String name() {
        return "trip_checklist";
    }

    @Override
    public String description() {
        return "Creates a pre-departure checklist covering documents, payments, devices, medicines, and travel essentials.";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "destination", Map.of("type", "string", "description", "Destination city or country"),
                        "trip_scope", Map.of(
                                "type", "string",
                                "enum", List.of("domestic", "international"),
                                "description", "Whether the trip is domestic or international"
                        )
                ),
                "required", List.of("destination", "trip_scope")
        );
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        String destination = String.valueOf(arguments.get("destination")).trim();
        String tripScope = String.valueOf(arguments.get("trip_scope")).trim().toLowerCase(Locale.ROOT);

        String internationalExtra = "international".equals(tripScope)
                ? """
                  - Passport and visa documents
                  - Currency / forex / card activation
                  - International roaming or eSIM check
                  - Travel insurance copy
                  """
                : """
                  - Valid government photo ID
                  - Local payment method and booking confirmations
                  """;

        return """
                Pre-departure checklist for %s
                Trip scope: %s

                Checklist:
                - Transport and hotel confirmations
                - ID documents
                - Charger, power bank, and adapters if needed
                - Medicines and first-aid basics
                - Cash plus at least one backup payment method
                - Weather-appropriate clothes and footwear
                %s
                - Share itinerary with one trusted contact
                - Keep emergency contact numbers accessible
                """.formatted(destination, tripScope, internationalExtra.stripTrailing());
    }
}
