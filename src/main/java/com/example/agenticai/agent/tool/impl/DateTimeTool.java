package com.example.agenticai.agent.tool.impl;

import com.example.agenticai.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/** Real (not mocked) tool - returns the actual current time for an IANA timezone. */
@Component
public class DateTimeTool implements Tool {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy HH:mm:ss z");

    @Override
    public String name() {
        return "get_current_time";
    }

    @Override
    public String description() {
        return "Returns the current date and time for a given IANA timezone id, e.g. Asia/Kolkata or America/New_York.";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "timezone", Map.of("type", "string", "description", "IANA timezone id, e.g. Asia/Kolkata")
                ),
                "required", List.of("timezone")
        );
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        String timezone = String.valueOf(arguments.get("timezone"));
        try {
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of(timezone));
            return now.format(FORMAT);
        } catch (DateTimeException e) {
            return "Invalid timezone id: '" + timezone + "'";
        }
    }
}
