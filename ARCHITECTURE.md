# Architecture

Pollen is a **private AI operating layer for Android**.

## Product identity

The primary identity is not mesh or agents. Those are implementation features.

The product is:

> The private AI layer that turns Android phones into autonomous assistants.

## Core loop

`Phone event -> brain decision -> action -> memory update`

## Main subsystems

### 1. OS layer
- foreground brain service
- notification listener
- call-screening scaffold
- boot receiver
- intent routing

### 2. Brain
- local LLM manager
- Gemini Nano adapter hook
- deterministic fallback model for offline testing
- decision normalization

### 3. Memory
- local-first memory direction
- event bus / state pipeline
- room for CRDT-style sync later if retained

### 4. Action layer
- app intents
- notification actions
- future SMS/call/calendar/camera hooks

### 5. Nearby cooperation
- peer discovery
- routing by capability
- trust-aware delegation
- optional transport abstraction

## Current status

This repo is intended for:
- local testing
- UX validation
- architecture feedback
- early privacy/security review

It is not yet a privileged Android system component or custom ROM.
