# Merge Map

## Product identity
Pollen is implemented as an **Android AI operating layer**. Mesh, CRDT, and swarm are treated as enabling systems, not the main product identity.

## Source carry-forward
- **v4**
  - foreground service pattern
  - boot persistence
  - runtime / mesh kernel concepts
  - on-device LLM ambition
- **v5**
  - CRDT memory concepts
  - hybrid logical clock
  - trust / node reputation ideas
  - Gemini Nano binding direction
- **v6-android**
  - Android shell and permissions model
  - event bus and intent routing patterns
  - tracing and dashboard direction

## New active architecture
- `core/` — decisions, events, memory, tracing
- `llm/` — local model provider abstraction
- `oslayer/` — service loop, event intake, action execution
- `swarm/` — peer discovery, trust, offline handoff
- `ui/` — test dashboard
- `legacy/` — preserved original code trees
