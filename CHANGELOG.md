# Changelog

All notable changes to Pollen are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [0.6.0] — 2026-04-19

### Added — AI OS Layer (`com.stackbleedctrl.pollen.oslayer`)
- `PollenBrainService` — always-running foreground service; boots on device start, persists via START_STICKY and BootReceiver
- `IntentRouter` — natural language → AppAction pipeline; keyword classifier + LLM fallback stub (MediaPipe); 7 intent categories; pending intent queue
- `BriefingComposer` — parallel coroutine reads from CallLog, SMS, and Calendar; privacy-first (metadata only, no message bodies); missed-day and periodic briefing modes
- `BrainEventBus` — SharedFlow-based event bus; sealed `BrainEvent` hierarchy (MissedDay, UrgentMessage, IntentExecuted, BriefingReady, SpamCallBlocked, CalendarConflict)

### Added — Public SDK (`com.stackbleedctrl.pollen.sdk`)
- `PollenSdk` with `BrainHandle` and `IntentHandle`
- `pollen.brain.handleMissedDay { briefing → }` — the primary catch-up callback
- `pollen.brain.handleUrgentMessage`, `handleBriefingReady`, `handleSpamBlocked`, `handleIntentExecuted`
- `pollen.intent.handle(rawText)` — fire-and-forget dispatch
- `pollen.intent.handleAndAwait(rawText)` — suspending dispatch returning `RoutingResult`

### Added — DI
- `PollenOsLayerModule` — Hilt bindings for all OS layer singletons
- `PollenApp` application class with brain service boot sequence

### Added — Project structure
- Migrated to standard Android project layout (`app/src/main/java/com/stackbleedctrl/pollen/`)
- `libs.versions.toml` version catalog
- ProGuard rules preserving SDK public surface and Room entities

### Existing (v5 → v6 carry-forward)
- `AdaptiveRouter` — UCB1 + ε-greedy mesh route selection
- `SybilDefence` — three-layer Sybil attack protection
- `PollenTracer` — lightweight distributed tracing with ring buffer
- `TraceScreen` — Compose in-app trace viewer
- `CrdtCompactor` — tombstone GC + OR-Set age compaction
- `CrdtMemoryStoreExtensions` — accessor extensions

---

## [0.5.0] — prior

See `POLLEN_V5.md` for v5 history.
