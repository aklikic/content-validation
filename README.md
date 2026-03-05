# Content Validation Service

A multi-agent pipeline for validating content through AI agents before routing to downstream systems. Includes a human-in-the-loop (HITL) review step when confidence is low or validation fails.

See [docs/SPEC.md](docs/SPEC.md) for the full architecture and API specification.

---

## Option 1: Run the sample locally

### Prerequisites

- Java 21+
- Maven 3.9+
- An OpenAI API key (or compatible provider)
- [Akka CLI](https://doc.akka.io/reference/cli/index.html) installed for the local console

### Start the service

```bash
OPENAI_API_KEY=<your-key> mvn compile exec:java
```

The service starts on `http://localhost:9000`.

### Open the UI

Open `http://localhost:9000` in your browser for the built-in UI with tabs for content submission and the review queue.

### Open the Akka Local Console

```bash
akka local console
```

This opens the Akka Local Console where you can inspect running components, view events, and monitor workflows.

### API

#### Submit content

```bash
curl -X POST http://localhost:9000/content \
  -H "Content-Type: application/json" \
  -d '{"contentId": "test-1", "payload": "Click here", "metadata": {}}'
```

#### Poll status

```bash
curl http://localhost:9000/content/test-1/status
```

#### Stream status updates (Server-Sent Events)

```bash
curl -N http://localhost:9000/content/test-1/stream
```

#### Triggering human review (HITL)

The workflow pauses at `AWAITING_REVIEW` when the aggregator returns low confidence (< 80%) or any validator fails.
See [docs/SPEC.md](docs/SPEC.md#test-content-examples) for example payloads that trigger HITL, the happy path, and guardrail rejection.

Once the status reaches `AWAITING_REVIEW`, submit a review decision to resume the workflow:

```bash
curl -X POST http://localhost:9000/reviews/hitl-1/decision \
  -H "Content-Type: application/json" \
  -d '{"decision": "APPROVE", "reviewer": "alice", "notes": "Looks fine"}'
```

Valid decisions: `APPROVE`, `REJECT`, `OVERRIDE`.

---

## Option 2: Implement it yourself with AI-assisted development

Build the service from scratch using Claude Code as your AI coding assistant.

### 1. Initialize a new Akka project

```bash
akka code init --template empty-project my-content-validation
cd my-content-validation
```

When prompted, choose **Claude** as your AI assistant.

### 2. Open the project in your IDE

```bash
idea .       # IntelliJ IDEA
# or
code .       # VS Code
```

### 3. Copy the design document into your project

```bash
cp /path/to/content-validation/docs/AKKA-DESIGN.md .
```

This document contains the full component design — entities, workflows, agents, views, and endpoints — ready for Claude to implement.

### 4. Start Claude Code and implement

```bash
claude
```

Then prompt Claude to implement the service from the design document:

```
Implement the service described in AKKA-DESIGN.md
```

Claude will guide you step-by-step through creating each component, running tests, and validating the implementation.