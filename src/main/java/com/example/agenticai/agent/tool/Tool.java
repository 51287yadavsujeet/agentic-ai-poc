package com.example.agenticai.agent.tool;

import java.util.Map;

/**
 * A capability the agent can invoke. Implementations are Spring beans - just add a new
 * @Component implementing this interface and the agent will automatically be able to
 * discover and call it, with no other wiring required.
 */
public interface Tool {

    /** Must match the pattern OpenAI expects: letters, numbers, underscores. */
    String name();

    /** Told to the model verbatim - be precise, this is how the model decides to call it. */
    String description();

    /** JSON-schema object describing the tool's parameters. */
    Map<String, Object> parameters();

    /** Executes the tool and returns a plain-text result to feed back to the model. */
    String execute(Map<String, Object> arguments);
}
