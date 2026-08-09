# Trip planner agentic flow

This file explains how the trip-planning flow works in this project and how to call it.
The trip flow is now a multi-tool plan:

- `plan_trip` for itinerary generation
- `search_flights` for mock flight options
- `get_hotel_details` for mock stay options
- `calculate_currency` for USD/INR conversion
- `estimate_trip_budget` for total trip budget estimation
- `search_trains` for mock train options
- `estimate_cab_fare` for transfer and local fare estimation
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

## 1. Entry points

Two controller endpoints can trigger the agent:

- `POST /api/agent/chat`
- `GET /api/agent/chat-browser?message=...`

Code:

- `AgentController.chat(...)`
- `AgentController.chatBrowser(...)`

Both endpoints forward the user message to:

```java
agentService.run(message)
```

## 2. Agent loop

`AgentService.run(...)` is the core orchestration flow.

It does the following:

1. creates a conversation with:
   - system prompt
   - user message
2. loads all available tool definitions from `ToolRegistry`
3. sends messages + tools to `OpenAiClient`
4. checks whether the model:
   - returned a final answer, or
   - requested one or more tool calls
5. if tool calls are requested:
   - parse tool arguments
   - execute the tool through `ToolRegistry`
   - append tool result back into the conversation
   - call the model again
6. repeat until a final answer is produced or max iterations is reached

## 3. Tool discovery

`ToolRegistry` automatically collects all Spring beans implementing `Tool`.

That means any class like this is auto-registered:

```java
@Component
public class TripPlannerTool implements Tool
```

No manual wiring is required in the controller or service.

## 4. Trip planner tool flow

When the user asks something like:

`Plan a 3-day mid-range trip to Goa focused on food and nightlife`

the model can choose one or more of these tools:

- `plan_trip`
- `search_flights`
- `get_hotel_details`
- `calculate_currency`
- `estimate_trip_budget`
- `search_trains`
- `estimate_cab_fare`
- `packing_help`
- `medical_help`
- `visa_requirements`
- `local_transport_help`
- `restaurant_suggestions`
- `places_to_visit`
- `trip_checklist`
- `expense_splitter`
- `trip_summary`
- `emergency_contacts_help`
- `language_help`
- `weather_forecast`

Each tool definition comes from the same `Tool` contract:

- `name()` -> returns `plan_trip`
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

`CurrencyConversionTool.execute(...)`:

1. reads amount, source currency, and target currency
2. converts between USD and INR
3. uses a static demo rate for deterministic budgeting output

`BudgetTool.execute(...)`:

1. reads destination, days, travelers, and budget style
2. estimates hotel, food, local travel, and sightseeing costs
3. returns a total INR budget with a category breakdown

`TrainSearchTool.execute(...)`:

1. reads origin, destination, departure date, and preferred class
2. matches the route against deterministic sample train options
3. returns train names, timings, and indicative fares

`CabFareTool.execute(...)`:

1. reads pickup, drop, distance, and cab type
2. applies static pricing rules
3. returns an INR fare estimate

`PackingHelpTool.execute(...)`:

1. reads destination, weather, days, and trip type
2. builds a checklist using weather-aware rules
3. adds trip-specific items for business, adventure, beach, family, or leisure travel

`MedicalHelpTool.execute(...)`:

1. reads destination, weather, trip type, and optional symptom
2. builds a basic travel medical-kit checklist
3. adds simple non-diagnostic medicine guidance for common travel issues

`VisaRequirementsTool.execute(...)`:

1. reads nationality and destination country
2. returns a static visa/passport checklist
3. reminds the user to verify official rules

`LocalTransportHelpTool.execute(...)`:

1. reads city and trip type
2. suggests metro, cabs, rental, or walkable patterns
3. returns a city-specific transport recommendation

`RestaurantSuggestionsTool.execute(...)`:

1. reads city, cuisine, and budget
2. matches a mock recommendation set
3. returns sample dining options

`PlacesToVisitTool.execute(...)`:

1. reads city, trip type, and days
2. suggests major attractions
3. adds a planning hint by trip style

`TripChecklistTool.execute(...)`:

1. reads destination and trip scope
2. returns a pre-departure checklist
3. includes passport/visa or domestic-ID emphasis as needed

`ExpenseSplitterTool.execute(...)`:

1. reads total amount, people, and optional contingency
2. calculates a per-person share
3. returns split totals

`TripSummaryTool.execute(...)`:

1. reads destination and optional section summaries
2. combines itinerary, transport, hotel, budget, packing, and medical details
3. returns one consolidated trip summary

`EmergencyContactsHelpTool.execute(...)`:

1. reads destination and trip scope
2. returns a practical emergency-contact checklist
3. adds international-specific contact categories where needed

`LanguageHelpTool.execute(...)`:

1. reads local language and context
2. returns a few useful phrases
3. supports general, transport, restaurant, and emergency contexts

`WeatherForecastTool.execute(...)`:

1. reads city and forecast days
2. returns a short multi-day mock forecast
3. helps packing and daily planning

## 6. End-to-end sequence

```text
Browser / Client
    ->
AgentController
    ->
AgentService.run(message)
    ->
OpenAiClient.chatCompletion(messages, tools)
    ->
Model decides whether to call one or more travel tools
    ->
ToolRegistry.execute(toolName, args)
    ->
Selected tool executes
    ->
Tool result returned to AgentService
    ->
Tool result added back to model conversation
    ->
Model generates final answer
    ->
ChatResponse returned to client
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

## 8. How to verify tool registration

Call:

```text
http://localhost:8080/api/agent/tools
```

You should see an entry like:

```json
{
  "name": "plan_trip",
  "description": "Creates a short trip plan with destination, trip duration, budget style, and suggested activities. Uses static planning logic for this demo."
}
```

And also:

```json
{
  "name": "search_flights",
  "description": "Searches mock flight options for a route and date, including indicative airline, timing, and fare. Useful for trip-planning demos."
}
```

```json
{
  "name": "get_hotel_details",
  "description": "Returns mock hotel suggestions for a city, with nightly budget guidance and amenities. Useful for building a full trip plan."
}
```

```json
{
  "name": "calculate_currency",
  "description": "Converts currency amounts between USD and INR using a static demo exchange rate. Best for quick travel budget estimation."
}
```

```json
{
  "name": "estimate_trip_budget",
  "description": "Estimates a total trip budget in INR based on destination, days, travelers, and budget style. Useful for high-level trip cost planning."
}
```

```json
{
  "name": "search_trains",
  "description": "Searches mock train options for a route and date, including train name, timing, and indicative fare. Useful for domestic travel planning."
}
```

```json
{
  "name": "estimate_cab_fare",
  "description": "Estimates cab fare in INR for a local or intercity ride using static demo pricing. Useful for transfer and local transport planning."
}
```

```json
{
  "name": "packing_help",
  "description": "Creates a packing checklist based on destination, weather conditions, trip duration, and trip type. Useful for travel planning after checking weather."
}
```

```json
{
  "name": "medical_help",
  "description": "Provides a basic travel medical and medicine checklist based on trip type, weather, and simple symptoms. It is for general preparedness only and not for diagnosis or emergency care."
}
```

```json
{
  "name": "visa_requirements",
  "description": "Provides a basic visa and passport checklist for travel based on traveler nationality and destination country. Uses static demo guidance, not legal advice."
}
```

```json
{
  "name": "local_transport_help",
  "description": "Suggests local transport options in a city such as metro, bus, cab, airport transfer, scooter rental, or walkable zones. Uses static demo guidance."
}
```

```json
{
  "name": "restaurant_suggestions",
  "description": "Provides mock restaurant suggestions by city, cuisine preference, and budget. Useful for itinerary and food-focused travel planning."
}
```

```json
{
  "name": "places_to_visit",
  "description": "Suggests places to visit based on city, trip type, and duration. Useful for itinerary construction."
}
```

```json
{
  "name": "trip_checklist",
  "description": "Creates a pre-departure checklist covering documents, payments, devices, medicines, and travel essentials."
}
```

```json
{
  "name": "expense_splitter",
  "description": "Splits a total trip expense across a group and optionally adds a contingency percentage."
}
```

```json
{
  "name": "trip_summary",
  "description": "Combines itinerary, transport, hotel, budget, packing, and medical notes into one structured trip summary."
}
```

```json
{
  "name": "emergency_contacts_help",
  "description": "Provides a practical emergency-contacts checklist for travel, including who to keep on hand before the trip."
}
```

```json
{
  "name": "language_help",
  "description": "Provides a few useful travel phrases for a selected local language, such as greeting, help, and transport phrases."
}
```

```json
{
  "name": "weather_forecast",
  "description": "Returns a short mock multi-day weather forecast for a city. Useful for packing and day planning."
}
```

## 9. Key files

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
