package com.example.agenticai.agent.tool.impl;

import com.example.agenticai.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class EmergencyContactsHelpTool implements Tool {

    @Override
    public String name() {
        return "emergency_contacts_help";
    }

    @Override
    public String description() {
        return "Provides a practical emergency-contacts checklist for travel, including who to keep on hand before the trip.";
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
                                "description", "Trip scope"
                        )
                ),
                "required", List.of("destination", "trip_scope")
        );
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        String destination = String.valueOf(arguments.get("destination")).trim();
        String tripScope = String.valueOf(arguments.get("trip_scope")).trim();
        String extra = "international".equalsIgnoreCase(tripScope)
                ? "- Embassy / consulate contact\n- Card issuer international helpline\n- Travel insurance emergency number"
                : "- Local family contact\n- State emergency or support helpline if relevant";

        return """
                Emergency contacts checklist for %s
                Trip scope: %s

                Keep these accessible:
                - Hotel or host contact
                - Airline / train / transport booking support
                - Local ambulance, police, and fire emergency numbers
                - One trusted personal contact
                %s
                """.formatted(destination, tripScope, extra);
    }
}
