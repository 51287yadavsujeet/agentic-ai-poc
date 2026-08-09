# Agentic AI POC

## Executive summary

This project is a Spring Boot 3 + JDK 21 proof of concept for an agentic AI application.

It demonstrates:

- model-driven tool calling
- iterative agent orchestration
- clean execution-trace responses
- virtual-thread-based request handling
- virtual-thread-based concurrent tool execution
- **comprehensive structured logging for complete code flow traceability**
- **decision-making visibility through strategic log prefixes**

The core business message is simple:

this POC shows how a Java application can move from plain chatbot behavior to guided task execution while remaining readable, observable, and scalable for blocking workloads.

---

## What the POC does

The application accepts a user message, sends it to the model along with all registered tool definitions, lets the model decide whether to answer directly or call tools, executes those tools in Java, feeds the results back to the model, and finally returns a human-readable answer with traceable steps.

Available APIs:

- `POST /api/agent/chat`
- `GET /api/agent/chat-browser?message=...`
- `GET /api/agent/tools`

Current domain focus:

- utility tools
- travel-planning tools
- travel-readiness tools

---

## Why this matters for a business demo

This POC demonstrates more than prompt/response AI.

It shows:

- the model can decide which internal capabilities to use
- application logic remains in your control
- execution is transparent through logs and JSON steps
- the architecture scales better for blocking I/O with virtual threads
- new capabilities can be added by implementing a single tool contract
- **complete visibility into agentic decision-making through structured logs**

---

## Current architecture

```text
Client
  ->
AgentController (logs request entry)
  ->
AgentService (orchestrates agent loop with decision logging)
  ->
OpenAiClient (calls model with tool definitions)
  ->
Model decides:
  - final answer
  - or one/more tool calls
  ->
ToolRegistry (executes and logs tool calls)
  ->
Tool implementations (individual tool execution)
  ->
tool results returned to AgentService
  ->
final ChatResponse with execution trace
```

---

## Core code components

### Entry and API layer

- `AgentController`
  - logs request entry with `[API_REQUEST]`
  - logs response completion with `[API_RESPONSE]`
  - calls `AgentService.run(...)`

### Orchestration layer

- `AgentService`
  - logs agent lifecycle with `[AGENT_ORCHESTRATION]`
  - logs each iteration with `[AGENT_LOOP]`
  - logs critical decisions with `[AGENT_DECISION]`
  - logs tool execution orchestration with `[AGENT_EXECUTION]`
  - builds the conversation
  - sends messages and tool definitions to the model
  - handles tool-call iterations
  - executes tools
  - records human-readable steps
  - returns `ChatResponse`

### Tool model

- `Tool`
  - standard interface for all tools

- `ToolRegistry`
  - logs registry initialization with `[TOOL_REGISTRY]`
  - logs tool execution with `[TOOL_EXECUTION]`
  - auto-discovers Spring `Tool` beans
  - exposes tool definitions to the model
  - executes tools by name

### OpenAI integration

- `OpenAiClient`
  - logs API calls with `[OPENAI_API_CALL]`
  - performs the model call to the OpenAI API
  - logs configuration and model decisions

### Response model

- `ChatResponse`
  - clean top-level response model

- `AgentStep`
  - human-readable execution trace per iteration

### Virtual-thread configuration

- `VirtualThreadConfig`
  - exposes `Executors.newVirtualThreadPerTaskExecutor()`

---

## End-to-end code flow

### Normal flow

1. client sends request to `/api/agent/chat` or `/api/agent/chat-browser`
2. `AgentController` logs the request with `[API_REQUEST]` and forwards the message
3. `AgentService` starts the agent run and logs with `[AGENT_ORCHESTRATION]`
4. `AgentService` creates:
   - system prompt
   - user message
5. `AgentService` calls `OpenAiClient.chatCompletion(...)` and logs with `[OPENAI_API_CALL]`
6. the model either:
   - returns a final answer, or
   - requests one or more tools
7. `AgentService` logs the decision with `[AGENT_DECISION]`
8. when tools are requested:
   - tool arguments are parsed
   - tools are executed through `ToolRegistry` with `[TOOL_CALL]` logging
   - results are added back into the conversation
   - a readable `AgentStep` is recorded
9. the model is called again
10. once a final answer is produced, a structured `ChatResponse` is returned
11. `AgentController` logs response completion with `[API_RESPONSE]`

### Safety behavior

- the loop stops after a maximum of 6 iterations
- if no final answer is produced, the response status becomes `incomplete`
- safety stops are logged with `[AGENT_ORCHESTRATION]` at WARN level

---

## Logging and Observability

### Key Log Categories

| Prefix | Level | Purpose | When Used |
|--------|-------|---------|-----------|
| `[API_REQUEST]` | INFO | HTTP request entry | Request received at controller |
| `[API_RESPONSE]` | INFO | Response completion | Response sent back to client |
| `[AGENT_ORCHESTRATION]` | INFO | Main agent lifecycle | Agent started/completed/safety limit |
| `[AGENT_LOOP]` | INFO | Iteration tracking | Each iteration begins |
| `[AGENT_DECISION]` | **INFO** | **Decision-making points** | **Tool selection vs final answer** |
| `[AGENT_EXECUTION]` | INFO | Tool execution orchestration | Tools executing, results collected |
| `[OPENAI_API_CALL]` | INFO | Model interactions | Model called, response received |
| `[TOOL_REGISTRY]` | DEBUG | Tool registration | Registry initialization |
| `[TOOL_EXECUTION]` | INFO | Tool lookup/execution | Tool executing, result received |
| `[TOOL_CALL]` | **INFO** | **Individual tool execution** | **Tool running on virtual thread** |
| `[TOOL_ASYNC]` | DEBUG | Async handling | Waiting for tool result |

---

## Current response format

The API now returns a clean business-readable JSON payload.

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
      "description": "The model requested tool 'get_weather' to gather information needed for the final answer.",
      "toolName": "get_weather",
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

Why this format is better:

- easier for demos
- easier for UI consumption
- clearer tool trace
- no raw JSON-string tool input fields

---

## Current tool inventory

### Utility tools

- `get_weather`
- `calculator`
- `get_current_time`
- `weather_forecast`

### Trip planning tools

- `plan_trip`
- `places_to_visit`
- `search_flights`
- `search_trains`
- `get_hotel_details`
- `local_transport_help`
- `estimate_cab_fare`
- `estimate_trip_budget`
- `calculate_currency`
- `expense_splitter`
- `restaurant_suggestions`
- `trip_summary`
- `trip_checklist`

### Travel readiness tools

- `packing_help`
- `medical_help`
- `visa_requirements`
- `emergency_contacts_help`
- `language_help`

Current tool discovery model:

- every tool is a Spring `@Component`
- every tool implements `Tool`
- registration is automatic through `ToolRegistry`

---

## Virtual thread usage in this project

Virtual threads are used in two distinct ways.

## 1. Virtual-thread request handling

Configured in:

```properties
spring.threads.virtual.enabled=true
```

Effect:

- Spring handles incoming HTTP requests on virtual threads
- blocking controller/service code stays simple
- the application is better suited for many concurrent blocking requests

## 2. Virtual-thread tool execution

Implemented in:

- `VirtualThreadConfig`
- `AgentService`

Executor used:

```java
Executors.newVirtualThreadPerTaskExecutor()
```

Behavior:

- when the model returns multiple tool calls in the same iteration
- each tool call is submitted to the virtual-thread executor
- those tool executions can run concurrently
- results are collected and added back into the conversation

This means the project is not only configured for virtual threads at the framework layer; it also uses them directly in application logic.

---

## Request/response flow without virtual threads

```text
HTTP request
  ->
platform thread
  ->
controller
  ->
service
  ->
blocking OpenAI call
  ->
blocking tool calls
  ->
response
```

Operational characteristics:

- one platform thread is occupied per blocking request
- blocked network waits hold expensive threads
- concurrency requires larger thread pools
- memory and scheduling overhead rise faster under load

---

## Request/response flow with virtual threads

```text
HTTP request
  ->
virtual thread
  ->
controller
  ->
service
  ->
blocking OpenAI call
  ->
tool calls on virtual threads
  ->
response
```

Operational characteristics:

- each request gets a lightweight virtual thread
- blocking code stays imperative and readable
- many more concurrent blocking tasks can be served efficiently
- independent tool work can run concurrently at lower thread cost

---

## Comparison summary

| Area | Without virtual threads | With virtual threads |
|---|---|---|
| Request thread model | Platform thread per request | Virtual thread per request |
| Blocking API wait | Holds platform thread | Holds virtual thread |
| Code style | Synchronous | Synchronous |
| Concurrency cost | Higher | Lower |
| Memory overhead under load | Higher | Lower |
| Tool fan-out | Sequential or more expensive concurrency | Cheaper concurrency |
| Operational simplicity | Simple | Simple |
| Fit for blocking AI workflows | Moderate | Better |

---

## Benefits of virtual threads here

### Better scalability for blocking workloads

This application blocks on:

- OpenAI API requests
- tool execution
- future external services such as weather, hotel, flight, or database calls

Virtual threads are a strong fit for this pattern.

### Keeps the code simple

The application remains easy to read:

- controller calls service
- service calls model
- model requests tools
- tools run in normal Java methods

No callback-heavy orchestration is required.

### Better fit for agentic tool loops

Agentic systems often need:

- repeated model calls
- external tool calls
- partial fan-out
- blocking waits

Virtual threads support this model well.

### Better demo and debugging story

The code flow remains easy to explain, and the logs clearly show:

- request entry
- virtual-thread usage
- tool-call flow
- final-answer generation

---

## Pros and cons

### Pros

- simple programming model
- better concurrency for blocking workflows
- lower overhead than many platform threads
- easy adoption in synchronous Spring code
- useful for multi-tool orchestration

### Cons

- not a CPU-speed optimization
- does not fix poor synchronization design
- benefit depends on blocking workloads being present
- tool concurrency only happens when the model asks for multiple tools in the same iteration
- some third-party libraries can reduce virtual-thread efficiency if they pin carrier threads

---

## Where virtual threads help most in this POC

Highest value:

- OpenAI request waits
- multiple tool calls in one iteration
- future live integrations for:
  - weather
  - hotels
  - flights
  - forex
  - databases

Lower value:

- static mock-data tools
- simple arithmetic
- pure CPU-only computations

---

## Logging and observability

The application now logs:

- controller request entry
- whether the current thread is virtual
- agent-run start
- each iteration start
- model finish reason
- number of tool calls requested
- tool name and parsed arguments
- tool completion
- final answer generation
- safety-stop behavior

This is useful for:

- demo walkthroughs
- debugging
- explaining the execution path
- validating virtual-thread usage

---

## Suggested demo flow

1. open `/api/agent/tools`
   - show the capability surface

2. call `/api/agent/chat-browser`
   - use a prompt that can trigger multiple tools

3. show the JSON response
   - highlight:
     - final answer
     - summary
     - tool-call steps

4. show the application logs
   - highlight:
     - virtual threads in request handling
     - tool execution on virtual threads
     - final-answer completion

5. explain the scale story
   - simple code
   - blocking-workload friendly
   - easier growth path to production integrations

---

## Current limitations

- travel data is mostly mock/static
- no persistence of conversation history
- no authentication or rate limiting
- no benchmark document yet
- `trip_summary` is a summary-composition tool, not a full internal orchestrator
- tool parallelism depends on the model returning multiple tool calls in one iteration

---

## Recommended next steps

### Short term

- add structured JSON output for individual tools
- add a dedicated `full_trip_plan` orchestration path
- add request correlation IDs in logs
- add a simple demo UI

### Medium term

- replace mock tools with live APIs
- add caching for repeated lookups
- add retry/fault handling
- add metrics for:
  - model latency
  - tool latency
  - iteration count
  - virtual-thread concurrency

### Long term

- production-grade travel orchestration
- user preferences and memory
- dashboards for observability and cost

---

## Final conclusion

This POC demonstrates a practical Java agentic architecture with:

- model-driven tool selection
- extensible Spring-managed tools
- structured response output
- detailed execution logs
- real virtual-thread adoption

The main technical conclusion is:

virtual threads let this application keep a clean synchronous design while improving concurrency behavior for blocking AI and tool workflows.
