# Contributing to Pollen

Thanks for your interest. Pollen is in active development — contributions are welcome, but please read this first.

## Before opening a PR

- Open an issue first for anything non-trivial. Large PRs without prior discussion will be closed.
- All new code must follow the existing architecture. New OS-layer capabilities go in `com.stackbleedctrl.pollen.oslayer`; new SDK surfaces go in `com.stackbleedctrl.pollen.sdk`.
- Privacy is non-negotiable. `BriefingComposer` reads metadata only — no PR that reads message bodies will be merged.

## Code style

- Kotlin only. No Java files.
- KDoc on all public classes and functions.
- Hilt for all dependency injection — no manual singletons outside of `PollenSdk.instance` (non-Hilt fallback only).
- Coroutines for all async work. No `Thread`, no `AsyncTask`, no `Handler`.

## Running tests

```bash
./gradlew test
./gradlew connectedAndroidTest
```

## Submitting

1. Fork, branch from `main`
2. Write tests for new logic
3. Run `./gradlew lint` — zero new warnings
4. Open PR with a clear description of what and why
