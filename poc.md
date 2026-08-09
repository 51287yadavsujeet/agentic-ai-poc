# Agentic AI POC with Virtual Threads

## 1. Executive summary

This proof of concept demonstrates a Spring Boot agentic application that:

- accepts a user request
- sends the request to an LLM with tool definitions
- lets the model decide which tools to call
- executes tools inside the application
- feeds tool results back to the model
- returns a final human-readable response with execution trace

The POC also uses Java virtual threads to improve concurrency for blocking request handling and tool execution while keeping the code simple and synchronous.

This is suitable for a business demo because it shows:

- agentic orchestration
- tool-based decisioning
- clean API responses
- observable execution flow
- scalable concurrency model

---

## 2. What this POC does

The application exposes agent endpoints such as:

- `POST /api/agent/chat`
- `GET /api/agent/chat-browser`
- `GET /api/agent/tools`

The agent can use multiple travel-oriented tools, including:

- weather
- trip planning
- hotels
- flights
- trains
- cab fare
- budget estimation
- packing help
- medical help
- restaurant suggestions
- local transport help
- visa checklist
- weather forecast
- trip summary

All tools are currently deterministic demo tools with static or mock data.

---

## 3. Business value

This POC demonstrates how an AI application can move beyond simple Q&A into guided task execution.

Business outcomes shown by this design:

- faster response assembly from multiple capabilities
- better user experience through structured answers
- strong explainability through step trace and logs
- easier scale for blocking workloads using virtual threads
- extensibility through plug-in style tools

---

## 4. High-level code flow

```text
Client request
   ->
AgentController
   ->
AgentService.run(userMessage)
   ->
OpenAiClient.chatCompletion(messages, toolDefinitions)
   ->
Model decides:
   - final answer
   - or one/more tool calls
   ->
ToolRegistry finds the tool implementation
   ->
Tool executes
   ->
Tool result is added back to conversation
   ->
Model produces final answer
   ->
ChatResponse returned to client
```

---

## 5. Main code components

### Entry layer

- `AgentController`
  - receives HTTP requests
  - logs request entry
  - forwards user message to service

### Orchestration layer

- `AgentService`
  - builds conversation
  - sends messages and tool definitions to the model
  - handles tool calls
  - records execution steps
  - returns clean response JSON

### Tool discovery and execution

- `Tool`
  - common interface for every tool

- `ToolRegistry`
  - auto-discovers all Spring `Tool` beans
  - exposes tool definitions to the model
  - executes requested tools by name

### OpenAI integration

- `OpenAiClient`
  - sends model requests
  - receives model tool calls or final answer

### Response model

- `ChatResponse`
  - top-level business-friendly response

- `AgentStep`
  - each execution step in a readable format

### Virtual thread configuration

- `VirtualThreadConfig`
  - provides a `newVirtualThreadPerTaskExecutor()`

---

## 6. Current request-response flow

### Step-by-step

1. user calls `/api/agent/chat` or `/api/agent/chat-browser`
2. controller logs the request
3. controller calls `agentService.run(message)`
4. `AgentService` creates:
   - system prompt
   - user message
5. `AgentService` sends:
   - messages
   - all tool definitions
   to the model
6. the model either:
   - answers directly, or
   - requests one or more tools
7. if tools are requested:
   - the tool arguments are parsed
   - tools are executed
   - results are recorded in the trace
   - tool results are added back into the conversation
8. the model is called again
9. final answer is returned as structured JSON

---

## 7. Virtual thread usage in this POC

Virtual threads are used in two places.

### 7.1 HTTP request handling

Configured in:

`application.properties`

```properties
spring.threads.virtual.enabled=true
```

Effect:

- Spring Boot serves HTTP requests on virtual threads
- each incoming request gets a lightweight thread
- blocking controller/service code remains simple

### 7.2 Tool execution

Configured in:

- `VirtualThreadConfig`
- `AgentService`

Implementation approach:

- a virtual-thread executor is created using:

```java
Executors.newVirtualThreadPerTaskExecutor()
```

- when the model requests multiple tools in the same iteration:
  - each tool call is submitted to the virtual-thread executor
  - tool executions can run concurrently
  - results are collected and added back into the model conversation

This means virtual threads are not just enabled at framework level; they are also used explicitly in application logic.

---

## 8. Without virtual threads vs with virtual threads

## Without virtual threads

```text
HTTP request
   ->
platform thread from server pool
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

Characteristics:

- each request occupies a platform thread
- blocked I/O keeps that expensive thread occupied
- higher concurrency requires more server threads
- memory and thread scheduling overhead are higher
- parallel tool execution is more costly

## With virtual threads

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

Characteristics:

- each request can run on a lightweight virtual thread
- blocking code remains readable
- many more concurrent blocking tasks can be handled efficiently
- independent tool calls can run concurrently at lower cost

---

## 9. Comparison table

| Area | Without virtual threads | With virtual threads |
|---|---|---|
| Request handling | Platform thread per request | Virtual thread per request |
| Blocking OpenAI/API wait | Occupies platform thread | Occupies virtual thread |
| Code style | Synchronous | Synchronous |
| Concurrency cost | Higher | Lower |
| Memory footprint under load | Higher | Lower |
| Thread pool tuning pressure | Higher | Lower |
| Multiple tool execution | Sequential or expensive parallelism | Cheap concurrent execution |
| Complexity vs reactive style | Low | Low |
| Scalability for blocking workloads | Moderate | Better |

---

## 10. Benefits of using virtual threads

### 10.1 Better scalability for blocking workloads

This app waits on:

- model API calls
- tool execution
- future external services

Virtual threads are designed for this kind of blocking workflow.

### 10.2 Keeps the code simple

The app remains imperative and readable:

- controller calls service
- service calls model
- tools execute normally

There is no need to convert the application into callback-heavy or reactive code just to improve concurrency.

### 10.3 Lower thread overhead

Platform threads are expensive compared to virtual threads.

With many simultaneous users:

- platform-thread usage scales poorly
- virtual threads scale more efficiently

### 10.4 Good fit for agentic workflows

Agentic flows often include:

- multiple blocking external calls
- repeated request/response loops
- tool fan-out patterns

Virtual threads are well-suited for these orchestration patterns.

### 10.5 Better business-demo observability

Because the code remains synchronous and readable:

- logging is easier to follow
- flow is easier to explain to non-engineering stakeholders
- technical architecture is easier to present

---

## 11. Pros and cons

### Pros

- simple programming model
- better concurrency for blocking workloads
- lower cost than large platform-thread pools
- easy adoption in existing synchronous Spring code
- useful for tool fan-out and orchestration
- clearer operational story for demos and scale discussions

### Cons

- not a performance boost for CPU-heavy logic
- does not solve bad locking or synchronization design
- some libraries may not behave ideally if they pin carrier threads
- tool execution still depends on how the model decides to call tools
- if the model requests tools sequentially across iterations, concurrency benefit is limited

---

## 12. When virtual threads help most in this POC

Highest-value areas:

- OpenAI API request wait time
- multiple tools requested in one iteration
- future live integrations:
  - flights
  - hotels
  - forex
  - weather APIs
  - databases

Lower-value areas:

- simple arithmetic
- static string generation
- CPU-only computations

---

## 13. Logging and observability now available

The application now logs:

- controller entry
- whether the current thread is virtual
- agent start
- each iteration start
- model finish reason
- number of tool calls requested
- each tool name and arguments
- each tool completion
- final answer generation
- safety-stop condition

This is useful for:

- debugging
- demo walkthroughs
- explaining the agent loop
- showing virtual-thread execution behavior

---

## 14. Clean response JSON for demo use

The response is now business-readable.

Example shape:

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

Why this is better:

- easier for humans to read
- easier for UI integration
- easier for API demo screens
- easier to explain execution steps

---

## 15. Demo talking points for business stakeholders

### What is special about this POC?

- it is not a simple chatbot
- it can decide when to use tools
- it shows transparent execution steps
- it is built to scale blocking workloads more efficiently with virtual threads

### Why does virtual thread usage matter?

- it improves concurrency without making the code harder to maintain
- it is a practical modernization path for Java applications
- it supports future growth when live APIs are introduced

### Why is this architecture extensible?

- every new capability is just a new `Tool`
- Spring auto-registers tools
- the model can decide how to use them

---

## 16. Suggested live demo flow

Recommended demo sequence:

1. show `/api/agent/tools`
   - demonstrates the available business capabilities

2. call `/api/agent/chat-browser`
   - ask for a travel plan that triggers multiple tools

3. show structured response JSON
   - highlight summary, tool calls, and final answer

4. show application logs
   - highlight:
     - virtual thread usage
     - tool execution flow
     - final response generation

5. explain scalability story
   - same readable code
   - better concurrency model
   - easier path to production-grade integrations

---

## 17. Current limitations

- tool data is mock/static, not live
- tool orchestration still depends on model decisions
- some tools may execute sequentially if the model requests them that way
- `trip_summary` currently combines provided sections; it does not independently orchestrate all other tools by itself
- no dedicated load test or benchmark is included yet

---

## 18. Recommended next steps

### Short term

- add structured JSON output for each tool
- add a dedicated `full_trip_plan` orchestration path
- add a small UI page for business demos
- add request correlation IDs in logs

### Medium term

- replace mock tools with live APIs
- add caching for repeated lookups
- add rate limits and fault handling
- add metrics for:
  - tool latency
  - model latency
  - iteration count
  - virtual-thread concurrency

### Long term

- production-grade travel orchestration
- user profile and preference memory
- cost controls and observability dashboards

---

## 19. Final conclusion

This POC demonstrates a clean Java/Spring agentic architecture with:

- model-driven tool usage
- extensible travel tools
- structured response output
- detailed execution logging
- real virtual-thread adoption

The main technical message is clear:

virtual threads let this application keep a simple synchronous design while improving concurrency behavior for blocking AI and tool workflows.
