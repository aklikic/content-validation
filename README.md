# Content Validation Service

A multi-agent pipeline for validating content through AI agents before routing to downstream systems. Includes a human-in-the-loop (HITL) review step when confidence is low or validation fails.

See [docs/SPEC.md](docs/SPEC.md) for the full architecture and API specification.

## Running

```bash
OPENAI_API_KEY=<your-key> mvn compile exec:java
```

The service starts on `http://localhost:9000`.

## UI

Open `http://localhost:9000` in your browser for the built-in UI with tabs for content submission and the review queue.

## API

### Submit content

```bash
curl -X POST http://localhost:9000/content \
  -H "Content-Type: application/json" \
  -d '{"contentId": "test-1", "payload": "Click here", "metadata": {}}'
```

### Poll status

```bash
curl http://localhost:9000/content/test-1/status
```

### Stream status updates (Server-Sent Events)

```bash
curl -N http://localhost:9000/content/test-1/stream
```

## Triggering human review (HITL)

The workflow pauses at `AWAITING_REVIEW` when the aggregator returns low confidence (< 80%) or any validator fails.
See [docs/SPEC.md](docs/SPEC.md#test-content-examples) for example payloads that trigger HITL, the happy path, and guardrail rejection.

Once the status reaches `AWAITING_REVIEW`, submit a review decision to resume the workflow:

```bash
curl -X POST http://localhost:9000/reviews/hitl-1/decision \
  -H "Content-Type: application/json" \
  -d '{"decision": "APPROVE", "reviewer": "alice", "notes": "Looks fine"}'
```

Valid decisions: `APPROVE`, `REJECT`, `OVERRIDE`.