# Multi-Model Concepts — Agentic AI POC

This document concisely describes the multi-model design decisions, operational guarantees, and extension points for the Agentic AI POC.

## 1. Purpose

- Provide an auditable, deterministic routing layer that chooses the best LLM (OpenAI or Google Gemini) for a given user request.
- Preserve tool-calling semantics (Java-executed tools) while leveraging each model's strengths.

## 2. Core principles

- Predictability: routing is keyword-driven and deterministic for reproducible behavior.
- Observability: each selection and fallback is logged with clear prefixes.
- Resilience: automatic, single-attempt fallback to the alternate provider when the preferred provider fails.
- Safety: Gemini requests do not include full tool payloads until format mapping is implemented.

## 3. Routing logic

- `ModelSelector` scans the user message for domain keywords (math, history, geography, medical).
- If a Gemini keyword is present, Gemini is preferred; otherwise OpenAI is used.
- Clients may override routing with `model` in the request JSON (`GEMINI` or `OPENAI`).

## 4. Fallback mechanics

- Preferred model attempted first.
- On exception, the agent logs a green console alert (`[FALLBACK ALERT]`) and attempts the alternate model once.
- If fallback also fails, a red error (`[FALLBACK FAILED]`) is logged; the exception propagates.
- The returned `ChatResponse.selectedModel` indicates the model that produced the final answer.

## 5. Tool-calling strategy

- `ToolRegistry` exposes function-like tool definitions to OpenAI; OpenAI returns function calls that the agent executes in Java.
- Gemini's API schema differs — to avoid schema errors we omit tool definitions in Gemini requests until a converter is implemented.
- Roadmap: implement schema conversion so Gemini can directly request tools.

## 6. Observability and logs

- Primary log prefixes:
  - `[MODEL_SELECTOR]` — model decision
  - `[OPENAI_API_CALL]`, `[GEMINI_API_CALL]` — API calls
  - `[AGENT_DECISION]`, `[AGENT_LOOP]` — agent lifecycle
  - `[TOOL_CALL]`, `[TOOL_EXECUTION]` — tool activity
  - `[AGENT_FALLBACK]`, `[FALLBACK ALERT]`, `[FALLBACK FAILED]` — fallback events

- The system writes a clear, colorized console alert when a fallback occurs for fast operator detection.

## 7. Extension points

- Replace keyword routing with a learned classifier or embedding-based router.
- Implement Gemini function-calling mapping (request and response translation).
- Add exponential backoff before fallback for transient 429 errors.
- Instrument metrics: model usage, latency, error rates, fallback counts.

## 8. Production guidance

- Never commit API keys — use secret managers.
- Monitor quotas for both providers and create alerts.
- Consider cost-aware routing: route inexpensive queries to the lower-cost provider.
- Add health checks and circuit-breaker logic before triggering fallback frequently.

## 9. Quick operational checklist

- Set `openai.api-key` and `gemini.api-key`.
- Test both models via the `model` override.
- Simulate failures to validate fallback logs and behavior.
- Review logs for `[FALLBACK ALERT]` after simulated failures.

---

Read `MULTI_MODEL_SETUP.md`, `MULTI_MODEL_EXAMPLES.md`, and `QUICK_REFERENCE.md` for setup and test cases.

The trip flow is a multi-tool orchestration where the model selects from 25+ tools:

- `trip_planner` for itinerary generation
- `flight_search` for mock flight options
- `hotel_details` for mock stay options
- `currency_conversion` for USD/INR conversion
- `budget` for total trip budget estimation
- `train_search` for mock train options
- `cab_fare` for transfer and local fare estimation
- `packing_help` for weather-aware packing suggestions
- `medical_help` for travel medical-kit and medicine guidance
- `visa_requirements` for passport and visa checklist
- `local_transport_help` for city transport suggestions
- `restaurant_suggestions` for cuisine-based dining suggestions
- `places_to_visit` for attractions and sightseeing ideas
- `trip_checklist` for pre-departure readiness
- `expense_splitter` for group cost sharing
- `trip_summary` for consolidated plan output
- `emergency_contacts_help` for emergency-contact preparation
- `language_help` for useful local phrases
- `weather_forecast` for multi-day mock forecast

The model's **decision to select which tools** is logged with `[AGENT_DECISION]` prefix for full visibility.

## 1. Entry points

Two controller endpoints can trigger the agent:

- `POST /api/agent/chat`
- `GET /api/agent/chat-browser?message=...`

Code:

- `AgentController.chat(...)` - logs with `[API_REQUEST]`
- `AgentController.chatBrowser(...)` - logs with `[API_REQUEST]`

Both endpoints forward the user message to:

```java
agentService.run(message)
```

## 2. Agent loop

`AgentService.run(...)` is the core orchestration flow.

It does the following:

1. logs agent start with `[AGENT_ORCHESTRATION]`
2. calls `ModelSelector` to determine which model to use:
   - **Gemini** if message contains: Math, History, Geography, Medical keywords
   - **OpenAI** otherwise (default)
   - logs decision with `[AGENT_ORCHESTRATION]` showing selected model
3. creates a conversation with:
   - system prompt
   - user message
4. loads all available tool definitions from `ToolRegistry`
5. logs iteration start with `[AGENT_LOOP]`
6. sends messages + tools to selected model:
   - `OpenAiClient` for OpenAI (logs with `[OPENAI_API_CALL]`)
   - `GeminiClient` for Gemini (logs with `[GEMINI_API_CALL]`)
7. checks whether the model:
   - returned a final answer, or
   - requested one or more tool calls
8. logs decision with `[AGENT_DECISION]` showing which option was chosen
9. if tool calls are requested:
   - logs tool selection details
   - parse tool arguments
   - execute the tool through `ToolRegistry` (logs with `[TOOL_CALL]`)
   - append tool result back into the conversation
   - call the model again
10. repeat until a final answer is produced or max iterations is reached
11. logs final result with `[AGENT_ORCHESTRATION]`
12. logs response completion with `[API_RESPONSE]`

## 3. Tool discovery

`ToolRegistry` automatically collects all Spring beans implementing `Tool`.

That means any class like this is auto-registered:

```java
@Component
public class TripPlannerTool implements Tool
```

No manual wiring is required in the controller or service.

The registry is logged during initialization with `[TOOL_REGISTRY]`.

## 4. Trip planner tool flow

When the user asks something like:

`Plan a 3-day mid-range trip to Goa focused on food and nightlife`

the model can choose one or more of these tools. The model's selection is logged with:
- `[AGENT_DECISION]` showing the tool names selected
- `[TOOL_CALL]` showing each tool executing with arguments

Each tool definition comes from the same `Tool` contract:

- `name()` -> returns `trip_planner`
- `description()` -> tells the model when to use it
- `parameters()` -> defines the JSON schema
- `execute()` -> generates the tool result

Expected tool arguments:

```json
{
  "destination": "Goa",
  "days": 3,
  "budget": "mid_range",
  "interests": ["food", "nightlife"]
}
```

## 5. What each tool does

`TripPlannerTool.execute(...)`:

1. reads input arguments
2. converts `days` to integer
3. formats optional interests
4. validates:
   - destination is not blank
   - days is greater than 0
5. builds a plain-text itinerary using static rules
6. adds a recommendation based on interests:
   - food
   - history
   - nature
   - shopping
   - nightlife
7. returns the generated trip plan as text

`FlightSearchTool.execute(...)`:

1. reads origin, destination, departure date, travelers, and cabin class
2. matches the route against a deterministic mock route table
3. returns sample airline options with indicative timings and fares

`HotelDetailsTool.execute(...)`:

1. reads city, budget, nights, and guests
2. matches city + budget against deterministic sample properties
3. returns mock hotel suggestions with nightly rates and amenities

And so on for other tools...

## 6. End-to-end sequence

```text
Browser / Client
    ->
AgentController (logs [API_REQUEST])
    ->
AgentService.run(message) (logs [AGENT_ORCHESTRATION])
    ->
ModelSelector.selectModel(message) (logs [MODEL_SELECTOR])
    -> Routes to:
       - OpenAiClient (travel/shopping/code queries)
       - GeminiClient (math/history/geography/medical queries)
    ->
Model API call (logs [OPENAI_API_CALL] or [GEMINI_API_CALL])
    ->
Model decides whether to call one or more trip tools
    ->
AgentService logs decision (logs [AGENT_DECISION])
    ->
ToolRegistry.execute(toolName, args) (logs [TOOL_EXECUTION])
    ->
Selected tool executes (logs [TOOL_CALL])
    ->
Tool result returned to AgentService
    ->
Tool result added back to model conversation
    ->
Model generates final answer
    ->
ChatResponse returned to client (logs [API_RESPONSE])
```

## 7. How to call it

### Browser

Open this URL:

```text
http://localhost:8080/api/agent/chat-browser?message=Plan%20a%203-day%20trip%20to%20Goa%20with%20places%20to%20visit%2C%20hotel%2C%20transport%2C%20budget%2C%20packing%2C%20medical%20help%2C%20restaurants%2C%20forecast%2C%20and%20a%20full%20summary
```

### PowerShell

```powershell
Invoke-RestMethod -Method POST `
  -Uri "http://localhost:8080/api/agent/chat" `
  -ContentType "application/json" `
  -Body '{"message":"Plan a 3-day trip to Goa with places to visit, hotel, transport, budget, packing, medical help, restaurants, forecast, and a full summary"}'
```

### curl.exe on Windows

```powershell
curl.exe -X POST "http://localhost:8080/api/agent/chat" `
  -H "Content-Type: application/json" `
  -d "{\"message\":\"Plan a 3-day trip to Goa with places to visit, hotel, transport, budget, packing, medical help, restaurants, forecast, and a full summary\"}"
```

## 8. Viewing the Logs

When you make a request, you'll see logs showing the complete flow:

```bash
grep MODEL_SELECTOR application.log   # View model selection decisions
grep AGENT_DECISION application.log   # View all model decisions
grep TOOL_CALL application.log        # View all tool executions
grep OPENAI_API_CALL application.log  # View OpenAI model interactions
grep GEMINI_API_CALL application.log  # View Gemini model interactions
```

Example log excerpt (Trip planning query routes to OpenAI):
```
[MODEL_SELECTOR] No special keywords detected | Selecting default OPENAI model
[AGENT_ORCHESTRATION] Model Selected: OPENAI | Reason: Default OpenAI
[OPENAI_API_CALL] Request Details | messageCount=2 | toolCount=25 | model='gpt-4o-mini'
[AGENT_DECISION] *** DECISION MADE: Execute Tools ***
[AGENT_DECISION] Iteration 1 will call 3 tool(s)
[AGENT_DECISION] Tool selected: 'trip_planner' with ID: 'call_xyz'
[AGENT_DECISION] Tool selected: 'flight_search' with ID: 'call_abc'
[TOOL_CALL] Tool Execution Result | Tool: 'trip_planner' | resultLength=500
[TOOL_CALL] Tool Execution Result | Tool: 'flight_search' | resultLength=300
```

Example log excerpt (Math query routes to Gemini):
```
[MODEL_SELECTOR] Keyword 'calculate' detected | Selecting GEMINI model
[AGENT_ORCHESTRATION] Model Selected: GEMINI | Reason: Keywords detected (Math/History/Geography/Medical)
[GEMINI_API_CALL] Request Details | messageCount=2 | toolCount=25 | model='gemini-2.0-flash'
[AGENT_DECISION] *** DECISION MADE: Execute Tools ***
```

## 9. How to verify tool registration

Call:

```text
http://localhost:8080/api/agent/tools
```

You should see entries for all 25+ tools including:

- trip_planner
- flight_search
- hotel_details
- currency_conversion
- budget
- train_search
- cab_fare
- packing_help
- medical_help
- visa_requirements
- local_transport_help
- restaurant_suggestions
- places_to_visit
- trip_checklist
- expense_splitter
- trip_summary
- emergency_contacts_help
- language_help
- weather_forecast

## 10. Key files

- `src/main/java/com/example/agenticai/controller/AgentController.java`
- `src/main/java/com/example/agenticai/agent/AgentService.java`
- `src/main/java/com/example/agenticai/agent/tool/ToolRegistry.java`
- `src/main/java/com/example/agenticai/agent/tool/Tool.java`
- `src/main/java/com/example/agenticai/agent/tool/impl/TripPlannerTool.java`
- `src/main/java/com/example/agenticai/agent/tool/impl/FlightSearchTool.java`
- `src/main/java/com/example/agenticai/agent/tool/impl/HotelDetailsTool.java`
- `src/main/java/com/example/agenticai/agent/tool/impl/CurrencyConversionTool.java`
- `src/main/java/com/example/agenticai/agent/tool/impl/BudgetTool.java`
- `src/main/java/com/example/agenticai/agent/tool/impl/TrainSearchTool.java`
- `src/main/java/com/example/agenticai/agent/tool/impl/CabFareTool.java`
- `src/main/java/com/example/agenticai/agent/tool/impl/PackingHelpTool.java`
- `src/main/java/com/example/agenticai/agent/tool/impl/MedicalHelpTool.java`
- `src/main/java/com/example/agenticai/agent/tool/impl/VisaRequirementsTool.java`
- `src/main/java/com/example/agenticai/agent/tool/impl/LocalTransportHelpTool.java`
- `src/main/java/com/example/agenticai/agent/tool/impl/RestaurantSuggestionsTool.java`
- `src/main/java/com/example/agenticai/agent/tool/impl/PlacesToVisitTool.java`
- `src/main/java/com/example/agenticai/agent/tool/impl/TripChecklistTool.java`
- `src/main/java/com/example/agenticai/agent/tool/impl/ExpenseSplitterTool.java`
- `src/main/java/com/example/agenticai/agent/tool/impl/TripSummaryTool.java`
- `src/main/java/com/example/agenticai/agent/tool/impl/EmergencyContactsHelpTool.java`
- `src/main/java/com/example/agenticai/agent/tool/impl/LanguageHelpTool.java`
- `src/main/java/com/example/agenticai/agent/tool/impl/WeatherForecastTool.java`
