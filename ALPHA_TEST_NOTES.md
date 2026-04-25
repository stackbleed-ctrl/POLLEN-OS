# POLLEN-OS Alpha Test Notes

## Current Alpha Build

- Build label: Alpha 0.3-dev
- Protocol: 0.2
- Task layer: 1
- AI layer: 1

## Confirmed Tests

- Two-device mesh connection
- Overnight mesh stability
- Cross-version mesh compatibility
- MESH_ECHO task routing
- DEVICE_STATUS task routing
- PING / PONG task routing
- FIELD_NOTE acknowledgement
- SIMULATED_HELP_SIGNAL acknowledgement
- Task latency measurement
- Task history and counters
- Trust / pairing v0
- Sensitive task guardrails
- Trusted-peer-only location snapshot guard
- AI decision layer v0
- AI mesh health score
- Peer compatibility check
- Compatibility-aware range probe
- Compatibility-aware demo sequence
- Tester log export upgrade

## Known Alpha Limitations

- Not production secure
- Not emergency certified
- Not a full Android replacement
- Older peer builds may not support all newer task types
- Protocol/version negotiation is early
- Location tasks require trusted peer state
- AI layer recommends only; it does not autonomously execute actions

## Recommended Demo Flow

1. Install current APK on two Android devices.
2. Open POLLEN-OS on both devices.
3. Tap Start Brain.
4. Wait for peer count to reach 1.
5. Tap Check Peer Compatibility.
6. Tap Run Demo Sequence.
7. Review Task Console.
8. Review AI Mesh Health.
9. Export Tester Log.
