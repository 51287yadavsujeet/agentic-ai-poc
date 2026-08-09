# Agentic AI POC

Spring Boot 3 + JDK 21 proof of concept for an agentic application that can call tools, observe results, and return a final answer with execution trace.

## What this project demonstrates

This project is not a simple prompt-to-response wrapper.

It runs an agent loop:

1. accept a user request
2. send the request and all available tool definitions to the model
3. let the model decide whether to answer directly or call one or more tools
4. execute tool calls inside the Java application
5. send tool results back to the model
6. repeat until the model returns a final answer or a safety limit is hit

It also demonstrates:

- Spring Boot REST APIs
- OpenAI tool-calling integration
- auto-discovered Java tools
- clean execution trace in API response
- virtual-thread request handling
- virtual-thread tool execution for concurrent tool calls
- **comprehensive structured logging for agentic decision-making**
- **complete code flow traceability**

## Current architecture

```text
Client
  ->
AgentController (logs API request entry)
  ->
AgentService (orchestrates agent loop)
  ->
OpenAiClient (calls model with tool definitions)
  ->
Model decides:
  - final answer
  - or tool calls
  ->
ToolRegistry (executes tools)
  ->
Tool implementations
  ->
results returned to AgentService
  ->
final ChatResponse with execution trace
```

## Project structure

```text
src/main/java/com/example/agenticai/
├── AgenticAiApplication.java
├── agent/
│   ├── AgentService.java
│   ├── AgentStep.java
│   └── tool/
│       ├── Tool.java
│       ├── ToolRegistry.java
│       └── impl/ (25+ tool implementations)
├── config/
│   ├── OpenAiProperties.java
│   └── VirtualThreadConfig.java
├── controller/
│   └── AgentController.java
├── dto/
│   ├── ChatRequest.java
│   └── ChatResponse.java
├── exception/
│   └── GlobalExceptionHandler.java
└── openai/
    ├── OpenAiClient.java
    └── model/
```

## Requirements

- JDK 21
- Maven 3.9+
- OpenAI API key

## Configuration

Main config file:

- [application.properties](/abs/path/C:/SUJEET/Java-code/agentic-ai-poc/src/main/resources/application.properties)

Key properties:

```properties
server.port=8080
spring.threads.virtual.enabled=true
openai.api-key=sk-proj-...
openai.model=${OPENAI_MODEL:gpt-4o-mini}
openai.base-url=https://api.openai.com/v1
logging.level.com.example.agenticai=DEBUG
logging.level.io.netty.util.internal=OFF
```

## How to run

### Option 1: Maven directly (recommended)

```bash
mvn spring-boot:run
```

### Option 2: Windows batch script

```bash
run.cmd
```

### Option 3: Build and run JAR

```bash
mvn clean package
java -jar target/agentic-ai-poc-0.0.1-SNAPSHOT.jar
```

App base URL:

```text
http://localhost:8080
```

## API endpoints

### 1. Chat API

`POST /api/agent/chat`

Request body:

```json
{
  "message": "What is the weather in Delhi?"
}
```

### 2. Browser-friendly chat API

`GET /api/agent/chat-browser?message=...`

Example:

```text
http://localhost:8080/api/agent/chat-browser?message=What%20is%20the%20weather%20in%20Delhi
```

### 3. Tool listing API

`GET /api/agent/tools`

Example:

```text
http://localhost:8080/api/agent/tools
```

## How to call the APIs

### PowerShell

```powershell
Invoke-RestMethod -Method POST `
  -Uri "http://localhost:8080/api/agent/chat" `
  -ContentType "application/json" `
  -Body '{"message":"Plan a 3-day trip to Goa with weather, hotel, budget, and packing help"}'
```

### curl.exe on Windows

```powershell
curl.exe -X POST "http://localhost:8080/api/agent/chat" `
  -H "Content-Type: application/json" `
  -d "{\"message\":\"Plan a 3-day trip to Goa with weather, hotel, budget, and packing help\"}"
```

### Direct browser demo

```text
http://localhost:8080/api/agent/chat-browser?message=Plan%20a%203-day%20trip%20to%20Goa%20with%20weather%2C%20hotel%2C%20budget%2C%20and%20packing%20help
```

## Current response format

The API returns a structured, business-readable JSON format.

Example:

```json
{
  "status": "success",
  "userMessage": "What is the weather in Delhi?",
  "answer": "The current weather in Delhi is 35C and hazy.",
  "summary": "Completed successfully after 2 iteration(s) with 1 tool call(s).",
  "totalIterations": 2,
  "totalToolCalls": 1,
  "steps": [
    {
      "iteration": 1,
      "stepType": "tool_call",
      "description": "The model requested tool 'weather' to gather information needed for the final answer.",
      "toolName": "weather",
      "toolArguments": {
        "city": "Delhi"
      },
      "output": "35C, hazy"
    },
    {
      "iteration": 2,
      "stepType": "final_answer",
      "description": "The model had enough information and returned the final human-readable answer.",
      "toolName": null,
      "toolArguments": null,
      "output": "The current weather in Delhi is 35C and hazy."
    }
  ]
}
```

## Current tools

The project contains a comprehensive demo travel toolset plus utility tools.

**Utility tools (8):**
- `calculator` - Basic arithmetic operations
- `datetime` - Current date and time
- `weather` - Weather for a location
- `weather_forecast` - Multi-day weather forecast

**Trip planning tools (12):**
- `trip_planner` - Trip itinerary generation
- `places_to_visit` - Attractions and sightseeing
- `flight_search` - Flight options
- `train_search` - Train options
- `hotel_details` - Hotel suggestions
- `currency_conversion` - USD/INR conversion
- `budget` - Trip budget estimation
- `cab_fare` - Cab fare estimation
- `local_transport_help` - City transport suggestions
- `restaurant_suggestions` - Dining recommendations
- `trip_summary` - Consolidated plan
- `trip_checklist` - Pre-departure checklist

**Travel readiness tools (5):**
- `packing_help` - Weather-aware packing list
- `medical_help` - Medical kit and medicine guidance
- `visa_requirements` - Visa/passport checklist
- `emergency_contacts_help` - Emergency contact preparation
- `language_help` - Useful local phrases

**See all tools:**

```text
http://localhost:8080/api/agent/tools
```

## How tools are added

Every tool implements:

- [Tool.java](/abs/path/C:/SUJEET/Java-code/agentic-ai-poc/src/main/java/com/example/agenticai/agent/tool/Tool.java)

Tool registration is automatic through Spring component discovery and:

- [ToolRegistry.java](/abs/path/C:/SUJEET/Java-code/agentic-ai-poc/src/main/java/com/example/agenticai/agent/tool/ToolRegistry.java)

Minimal example:

```java
@Component
public class MyTool implements Tool {

    @Override
    public String name() {
        return "my_tool";
    }

    @Override
    public String description() {
        return "Describe exactly what the tool does.";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "input", Map.of("type", "string")
                ),
                "required", List.of("input")
        );
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        return "result for " + arguments.get("input");
    }
}
```

## Logging and observability

### Structured logging with prefixes

All logs use consistent prefixes for easy filtering and analysis:

| Prefix | Level | Purpose |
|--------|-------|---------|
| `[API_REQUEST]` | INFO | HTTP request entry points |
| `[AGENT_ORCHESTRATION]` | INFO | Main agent lifecycle |
| `[AGENT_LOOP]` | INFO | Each iteration tracking |
| `[AGENT_DECISION]` | INFO | **Where decisions are made** (tool vs answer) |
| `[AGENT_EXECUTION]` | INFO | Tool execution orchestration |
| `[OPENAI_API_CALL]` | INFO | OpenAI model interactions |
| `[TOOL_REGISTRY]` | DEBUG | Tool registry management |
| `[TOOL_EXECUTION]` | INFO | Tool lookup and execution |
| `[TOOL_CALL]` | INFO | **Individual tool execution** |
| `[TOOL_ASYNC]` | DEBUG | Async/virtual thread handling |

### Example log trace

```
[API_REQUEST] ========== POST /api/agent/chat RECEIVED ==========
[API_REQUEST] Request Details | virtualThread=false | message='2+2?'
[AGENT_ORCHESTRATION] ========== AGENT RUN STARTED ==========
[AGENT_LOOP] ========== ITERATION 1 START ==========
[AGENT_DECISION] Calling OpenAI model to determine next action...
[OPENAI_API_CALL] ========== CALLING OPENAI API ==========
[AGENT_DECISION] *** DECISION MADE: Execute Tools ***
[AGENT_DECISION] Tool selected: 'calculator' with ID: 'call_123'
[TOOL_CALL] ========== EXECUTING TOOL ==========
[TOOL_CALL] Iteration 1 | Tool: 'calculator' | result=4
[AGENT_LOOP] ========== ITERATION 2 START ==========
[AGENT_DECISION] *** DECISION MADE: Return Final Answer ***
[AGENT_ORCHESTRATION] ========== AGENT RUN COMPLETED SUCCESSFULLY ==========
[API_RESPONSE] Chat request completed | status='success' | iterations=2 | toolCalls=1
```

### View logs with grep

```bash
# View all decision-making
grep AGENT_DECISION application.log

# View all tool executions
grep TOOL_CALL application.log

# View OpenAI interactions
grep OPENAI_API_CALL application.log

# View errors only
grep ERROR application.log
```

For complete logging documentation, see:
- [LOGGING_GUIDE.md](/abs/path/C:/SUJEET/Java-code/agentic-ai-poc/LOGGING_GUIDE.md)
- [LOGGING_IMPLEMENTATION_SUMMARY.md](/abs/path/C:/SUJEET/Java-code/agentic-ai-poc/LOGGING_IMPLEMENTATION_SUMMARY.md)

## Virtual thread usage

This project uses virtual threads in two ways.

### 1. HTTP request handling

Enabled by:

```properties
spring.threads.virtual.enabled=true
```

This lets Spring handle requests on virtual threads.

### 2. Tool execution

Configured in:

- [VirtualThreadConfig.java](/abs/path/C:/SUJEET/Java-code/agentic-ai-poc/src/main/java/com/example/agenticai/config/VirtualThreadConfig.java)

The app creates:

```java
Executors.newVirtualThreadPerTaskExecutor()
```

`AgentService` uses it to execute multiple tool calls concurrently when the model requests more than one tool in the same iteration.

### Why this matters

Benefits:

- better scalability for blocking workloads
- simpler code than reactive/callback-heavy orchestration
- lower thread overhead under concurrent requests
- better fit for model + tool workflows that block on I/O

Limitations:

- not a CPU performance optimization
- benefit is highest when requests block on network or I/O
- if the model requests tools sequentially across iterations, those iterations still remain sequential

For a fuller explanation, see:

- [poc.md](/abs/path/C:/SUJEET/Java-code/agentic-ai-poc/poc.md)

## Demo files

Additional project documentation:

- [trip.md](/abs/path/C:/SUJEET/Java-code/agentic-ai-poc/trip.md) - Trip planning flow
- [poc.md](/abs/path/C:/SUJEET/Java-code/agentic-ai-poc/poc.md) - Architecture and design
- [LOGGING_GUIDE.md](/abs/path/C:/SUJEET/Java-code/agentic-ai-poc/LOGGING_GUIDE.md) - Complete logging reference

## Known limitations

- Most travel tools use mock/static demo data
- No persistence of conversation history
- No authentication or rate limiting
- Final orchestration depends on model tool-calling behavior

## Recommended next steps

- Add structured JSON output per tool
- Add a dedicated orchestration layer for complex trip plans
- Replace mock data with live APIs
- Add metrics, tracing, and request correlation IDs
- Add observability dashboards
- Add a demo UI
