![Pollen-OS Banner](./pollen-os-banner.png)

🌼 Pollen OS

**The private AI layer that turns Android phones into autonomous, cooperative agents.**

> Runs on-device • Works offline • Mesh-native autonomous agents

---

⚡ What is Pollen OS?

Pollen OS is a decentralized, on-device AI system that enables Android devices to think locally, communicate directly, and cooperate without relying on the cloud.

It transforms Android devices into:

- 🧠 Autonomous agents (local AI decisions)
- 📡 Mesh-connected nodes (device-to-device communication)
- 🤝 Cooperative systems (shared intelligence across devices)

No cloud required. No central server.

---
## 🧬 Core Idea

Modern AI depends on centralized infrastructure.

Pollen OS flips that model:

- devices think locally
- devices communicate directly
- intelligence emerges from the network

This is private, edge-native, offline-capable AI infrastructure.


---

## 🚀 What it does today

- 🧠 **PollyBrain**: local decision engine that evaluates events and determines actions
- 🔌 **On-device LLM support**: integrates local model runtimes through `LocalLlmManager` and `GeminiNanoAdapter`
- 📡 **Mesh communication**: enables device-to-device coordination without centralized infrastructure
- 📴 **Offline-first operation**: core behaviors continue without cloud connectivity
- 🔒 **Private by design**: data and inference stay on the device whenever possible
---

🔬 Example: Two Devices, No Internet

1. Device A receives an event (notification, context, etc.)
2. Local AI evaluates it
3. Intent is broadcast to nearby devices
4. Device B receives + evaluates independently
5. Devices coordinate behavior

👉 Fully offline. Fully local.

---

🧪 Demo

See: "DEMO.md"

Quick start:

- Install on 2 Android devices
- Enable permissions
- Trigger an event
- Watch cross-device behavior

---

🧱 Architecture

User / System Events
        ↓
   PollyBrain (AI decisions)
        ↓
   BrainEventBus (routing)
        ↓
 SwarmCoordinator (multi-device logic)
        ↓
 NearbyMeshCoordinator (peer network)
        ↓
     Other Devices

---

📁 Key Components

- "PollyBrain.kt" → decision engine
- "SwarmCoordinator.kt" → distributed coordination
- "NearbyMeshCoordinator.kt" → mesh networking
- "LocalLlmManager.kt" → on-device AI
- "BrainEventBus.kt" → internal messaging

---

🛡️ Privacy First

- All processing is on-device
- No cloud dependency
- No centralized data collection

---

⚠️ Status

Early-stage prototype.

Core systems are functional, but evolving rapidly.

---

## 🌍 Why it matters

Most AI systems depend on cloud APIs, centralized servers, and constant connectivity.

Pollen OS takes a different path:
- private by default
- resilient when offline
- cooperative across nearby devices
- built for edge-native autonomy

This makes it useful for unreliable networks, privacy-sensitive use cases, and future peer-to-peer AI systems.

---

🤝 Contributing

See "CONTRIBUTING.md"

---

📡 Security

Report vulnerabilities via:

- GitHub private security advisory

---

📜 License

MIT
