# Agentic AI POC — Spring Boot 3 + JDK 21 + OpenAI

A small, self-contained proof of concept showing how to build an **agentic AI loop** in
Spring Boot: instead of one prompt → one answer, the app lets the LLM decide to call
tools (functions), observes the results, and keeps reasoning until it's ready to give a
final answer — all orchestrated by your own Java code.

## What "agentic" means here

A plain chatbot call is: `user message → model → text answer`.

This app instead runs a loop:

```
 1. Send user goal + system prompt + list of available tools to the model
 2. Model responds with either:
       a) a tool call (name + JSON arguments)  --> execute it in Java, feed the
          result back as a new message, go to step 1
       b) a final natural-language answer      --> return it, loop ends
 3. Repeat until (b), or until a safety cap (6 iterations) is hit
```

The model — not your code — decides *which* tool(s) to call, *in what order*, and
*when it has enough information to stop*. That decision-making loop is what makes it
"agentic" rather than a single request/response wrapper.

```
┌─────────────┐   POST /api/agent/chat    ┌────────────────┐
│   Client     │ ────────────────────────▶│ AgentController │
└─────────────┘                            └────────┬───────┘
                                                      │
                                                      ▼
                                            ┌────────────────┐        loop until
                                            │  AgentService   │◀──────  final answer
                                            └────────┬───────┘
                                     asks the model   │  executes tool
                                                      ▼
                            ┌───────────────┐   ┌──────────────┐
                            │ OpenAiClient   │   │ ToolRegistry │
                            │ (WebClient →   │   └──────┬───────┘
                            │  OpenAI API)   │          │
                            └───────────────┘   ┌───────┴────────┬────────────────┐
                                                 ▼                ▼                ▼
                                         CalculatorTool    WeatherTool       DateTimeTool
                                          (deterministic)   (mocked)        (real java.time)
```

## Project layout

```
src/main/java/com/example/agenticai/
├── AgenticAiApplication.java        Spring Boot entry point
├── config/OpenAiProperties.java     Binds openai.* from application.yml
├── openai/
│   ├── OpenAiClient.java            WebClient call to /chat/completions
│   └── model/                       Records mirroring OpenAI's JSON wire format
├── agent/
│   ├── AgentService.java            <-- the agentic loop lives here
│   ├── AgentStep.java               One entry in the reasoning trace
│   └── tool/
│       ├── Tool.java                Contract every tool implements
│       ├── ToolRegistry.java        Auto-discovers all Tool beans
│       └── impl/                    CalculatorTool, WeatherTool, DateTimeTool
├── controller/AgentController.java  REST endpoints
├── dto/                             ChatRequest / ChatResponse
└── exception/GlobalExceptionHandler.java
```

## Requirements

- JDK 21
- Maven 3.9+
- An OpenAI API key (https://platform.openai.com/api-keys) with access to a
  chat-completions model that supports tool calling (default here: `gpt-4o-mini`)

## Run it

```bash
export OPENAI_API_KEY=sk-your-key-here
# optional: export OPENAI_MODEL=gpt-4o-mini

mvn spring-boot:run
```

The app starts on `http://localhost:8080`.

## Try it

List the tools the agent currently has access to:

```bash
curl http://localhost:8080/api/agent/tools
```

Ask something that requires **chaining two tools** — a good way to see the agentic loop
in action (it should call `get_weather` for both cities, then `calculator` or reason
over the results itself):

```bash
curl -s -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "What is the weather in Pune and Mumbai, and which one is warmer?"}' | jq
```

Ask something requiring the calculator tool:

```bash
curl -s -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "What is 342 multiplied by 17, then divided by 3?"}' | jq
```

Ask for the current time in a specific timezone:

```bash
curl -s -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "What time is it right now in Tokyo?"}' | jq
```

### Example response shape

```json
{
  "answer": "It's currently around 11:42 PM in Tokyo.",
  "trace": [
    {
      "iteration": 1,
      "type": "tool_call",
      "toolName": "get_current_time",
      "toolInput": "{\"timezone\":\"Asia/Tokyo\"}",
      "output": "Sunday, 09 Aug 2026 23:42:10 JST"
    },
    {
      "iteration": 2,
      "type": "final_answer",
      "toolName": null,
      "toolInput": null,
      "output": "It's currently around 11:42 PM in Tokyo."
    }
  ]
}
```

The `trace` array is the whole point of the demo — it shows every tool call the agent
made and why, not just the final text.

## Adding your own tool

Implement `Tool` and annotate it `@Component` — the registry (and therefore the agent
and OpenAI) picks it up automatically, no other wiring needed:

```java
@Component
public class MyTool implements Tool {
    public String name() { return "my_tool"; }
    public String description() { return "What this tool does, precisely."; }
    public Map<String, Object> parameters() {
        return Map.of(
            "type", "object",
            "properties", Map.of("input", Map.of("type", "string")),
            "required", List.of("input")
        );
    }
    public String execute(Map<String, Object> arguments) {
        return "result for " + arguments.get("input");
    }
}
```

## JDK 21 features used

- **Records** for every DTO and OpenAI wire-format model (`ChatMessage`, `AgentStep`, etc.)
- **Pattern matching for `switch`**, including a `null` case, in `CalculatorTool`
- **Virtual threads** enabled for request handling (`spring.threads.virtual.enabled: true`
  in `application.yml`) — useful here because each agent turn blocks on an HTTP call to
  OpenAI, and virtual threads make that cheap to do per-request
- **Text blocks** for the system prompt

## Notes / things a production version would need

- This POC calls `OpenAiClient` synchronously (`.block()`) for simplicity — a production
  version would go fully reactive or use async endpoints.
- No conversation persistence — each `/chat` call starts a fresh reasoning session.
- No retry/backoff around the OpenAI call.
- `WeatherTool` is mocked; swap in a real HTTP client the same way `OpenAiClient` calls
  OpenAI.
- Add authentication/rate limiting before exposing `/api/agent/chat` publicly — an LLM
  with tool access is effectively code execution on your behalf.
