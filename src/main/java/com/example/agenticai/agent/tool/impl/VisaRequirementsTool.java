package com.example.agenticai.agent.tool.impl;

import com.example.agenticai.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class VisaRequirementsTool implements Tool {

    @Override
    public String name() {
        return "visa_requirements";
    }

    @Override
    public String description() {
        return "Provides a basic visa and passport checklist for travel based on traveler nationality and destination country. Uses static demo guidance, not legal advice.";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "nationality", Map.of("type", "string", "description", "Traveler nationality, e.g. Indian"),
                        "destination_country", Map.of("type", "string", "description", "Destination country, e.g. Thailand")
                ),
                "required", List.of("nationality", "destination_country")
        );
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        String nationality = String.valueOf(arguments.get("nationality")).trim();
        String destinationCountry = String.valueOf(arguments.get("destination_country")).trim();
        String key = (nationality + "-" + destinationCountry).toLowerCase(Locale.ROOT);

        String guidance = switch (key) {
            case "indian-thailand" -> "Passport validity of at least 6 months, return ticket, accommodation proof, and check latest visa-on-arrival or e-visa rules.";
            case "indian-dubai", "indian-uae" -> "Passport validity of at least 6 months, confirmed hotel, return ticket, travel insurance, and check current UAE visa requirements.";
            case "indian-singapore" -> "Passport validity of at least 6 months, onward/return ticket, accommodation details, and check current Singapore visa application requirements.";
            case "indian-nepal" -> "Carry valid government identification or passport and verify current cross-border document rules before travel.";
            case "indian-india" -> "No visa required for domestic travel. Carry valid government ID.";
            default -> "Carry a passport with sufficient validity, return/onward ticket, accommodation proof, financial proof if needed, and verify the latest visa rules from official sources before travel.";
        };

        return """
                Visa and passport checklist
                Nationality: %s
                Destination: %s

                Guidance:
                %s

                Safety note: This is static demo guidance only. Always verify official immigration rules before booking.
                """.formatted(nationality, destinationCountry, guidance);
    }
}
