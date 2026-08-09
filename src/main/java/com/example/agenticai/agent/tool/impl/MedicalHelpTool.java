package com.example.agenticai.agent.tool.impl;

import com.example.agenticai.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class MedicalHelpTool implements Tool {

    @Override
    public String name() {
        return "medical_help";
    }

    @Override
    public String description() {
        return "Provides a basic travel medical and medicine checklist based on trip type, weather, and simple symptoms. It is for general preparedness only and not for diagnosis or emergency care.";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "destination", Map.of("type", "string", "description", "Destination city or region"),
                        "weather", Map.of("type", "string", "description", "Weather summary such as 35C, hazy or 24C, light rain"),
                        "trip_type", Map.of(
                                "type", "string",
                                "enum", List.of("leisure", "business", "adventure", "beach", "family"),
                                "description", "Primary trip type"
                        ),
                        "symptom", Map.of(
                                "type", "string",
                                "description", "Optional basic symptom such as headache, cold, motion sickness, fever, allergy, stomach upset"
                        )
                ),
                "required", List.of("destination", "weather", "trip_type")
        );
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        String destination = String.valueOf(arguments.get("destination")).trim();
        String weather = String.valueOf(arguments.get("weather")).trim().toLowerCase(Locale.ROOT);
        String tripType = String.valueOf(arguments.get("trip_type")).trim().toLowerCase(Locale.ROOT);
        String symptom = String.valueOf(arguments.getOrDefault("symptom", "")).trim().toLowerCase(Locale.ROOT);

        if (destination.isBlank()) {
            return "Destination is required.";
        }

        List<String> kit = new ArrayList<>();
        kit.add("personal prescription medicines in original packaging");
        kit.add("doctor prescriptions or medication list");
        kit.add("small first-aid kit");
        kit.add("bandages and antiseptic wipes");
        kit.add("pain reliever suitable for the traveler");
        kit.add("hand sanitizer");

        if (weather.contains("rain") || weather.contains("showers")) {
            kit.add("mosquito repellent");
            kit.add("antifungal or skin-protection cream if needed for humid conditions");
        }
        if (weather.contains("35c") || weather.contains("31c") || weather.contains("28c") || weather.contains("sun")) {
            kit.add("ORS or hydration sachets");
            kit.add("sunscreen and lip balm");
        }
        if (weather.contains("hazy") || weather.contains("dust")) {
            kit.add("mask for dust-sensitive travelers");
            kit.add("saline nasal spray if normally used");
        }

        switch (tripType) {
            case "adventure" -> {
                kit.add("blister pads");
                kit.add("muscle pain spray or gel");
                kit.add("extra crepe bandage");
            }
            case "beach" -> {
                kit.add("after-sun lotion");
                kit.add("waterproof bandages");
            }
            case "family" -> {
                kit.add("thermometer");
                kit.add("child-safe medicines if relevant");
            }
            default -> {
            }
        }

        String symptomAdvice = symptomAdvice(symptom);

        return """
                Medical and medicine help for %s
                Weather context: %s
                Trip type: %s
                Symptom focus: %s

                Suggested medical kit:
                - %s

                Basic symptom guidance:
                %s

                Safety note: This is general travel-preparedness guidance only. For severe symptoms, prescription advice, or emergencies, contact a qualified clinician or local emergency services.
                """.formatted(
                destination,
                weather,
                tripType,
                symptom.isBlank() ? "general travel kit" : symptom,
                String.join("\n- ", kit),
                symptomAdvice
        );
    }

    private String symptomAdvice(String symptom) {
        if (symptom.isBlank()) {
            return "- No symptom specified; prepare a general travel medical kit.";
        }
        if (symptom.contains("headache")) {
            return "- Carry your usual pain reliever, stay hydrated, and rest if needed.";
        }
        if (symptom.contains("cold") || symptom.contains("cough")) {
            return "- Carry throat lozenges, tissues, and any usual cold medicine you already use.";
        }
        if (symptom.contains("motion")) {
            return "- Carry your usual motion-sickness medicine and avoid heavy meals before travel.";
        }
        if (symptom.contains("fever")) {
            return "- Carry a thermometer and your usual fever medicine; seek clinical advice if fever is persistent or high.";
        }
        if (symptom.contains("allergy")) {
            return "- Carry your prescribed or usual allergy medicine and avoid known triggers.";
        }
        if (symptom.contains("stomach") || symptom.contains("digest") || symptom.contains("loose")) {
            return "- Carry ORS and any usual stomach-relief medicine you already tolerate well.";
        }
        return "- Use only medicines already known to be safe for the traveler and seek a clinician if symptoms worsen.";
    }
}
