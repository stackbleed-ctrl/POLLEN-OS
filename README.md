# Pollen

![Pollen launch banner](assets/pollen-launch-banner.png)

**Pollen is a private AI operating layer for Android.**

It is designed to turn Android phones into autonomous assistants that run on-device, work offline, and can optionally cooperate with nearby devices.

## What this repo is

This is the **clean launch repo** for early testers.

It is structured for:
- GitHub publishing
- Android Studio import
- device testing
- issue collection
- rapid iteration

## Product identity

Pollen is not being positioned as “mesh” or “agents” first.

The product is:

> **The private AI layer that turns Android phones into autonomous assistants.**

Nearby cooperation is an implementation advantage, not the headline.

## Included right now

### Android operating layer
- foreground brain service
- notification listener scaffold
- call-screening scaffold
- boot receiver
- Compose dashboard

### Local intelligence
- local LLM manager abstraction
- Gemini Nano adapter hook
- deterministic offline fallback
- memory + event pipeline foundations

### Nearby cooperation
- peer/swarm scaffolding
- routing abstractions
- trust-aware delegation direction

### Launch kit
- architecture docs
- roadmap
- privacy note
- security stub
- GitHub issue templates
- tester feedback guide

## Honest status

This is a strong **Android app/service foundation** for a private AI operating layer.

It is **not yet**:
- a custom Android OS
- a privileged ROM component
- a production-signed release

## Quick start

1. Open in Android Studio.
2. Sync Gradle.
3. Run the debug build on one Android device.
4. Grant only the permissions needed for your chosen test.
5. Start the foreground brain service.
6. Repeat on a second device for nearby testing.

See [INSTALL.md](INSTALL.md) for setup details.

## Best tester prompts

- `summarize my notifications`
- `open maps`
- `Check the weather and send a summary via SMS to my partner if it's raining`
- `Use the camera on the nearest device with good lighting to describe what's in front of me`

## Model planning note

A realistic planning target for a 2B-class on-device model is:
- about **1.3 GB** download size for an int4 package
- about **2–3 GB RAM** on capable phones during active inference

Exact footprint depends on the final backend and device.

## Feedback requested

We want feedback on:
- usefulness
- trust
- battery impact
- permission comfort
- nearby multi-device behavior

Start here:
- [docs/FEEDBACK_GUIDE.md](docs/FEEDBACK_GUIDE.md)
- [docs/TEST_PLAN.md](docs/TEST_PLAN.md)
- [docs/LAUNCH_CHECKLIST.md](docs/LAUNCH_CHECKLIST.md)

## Repo structure

- `app/` active Android app
- `docs/` launch and architecture docs
- `assets/` launch artwork
- `metadata/` packaging metadata
- `.github/` issue templates and workflow

## License

MIT
