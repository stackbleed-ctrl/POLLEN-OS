# Security Policy

## Supported versions

Only the latest release receives security fixes.

## Reporting a vulnerability

**Do not open a public GitHub issue for security vulnerabilities.**

Email: stackbleed@gmail.com (or open a private GitHub advisory)

Include:
- Description of the vulnerability
- Steps to reproduce
- Affected component
- Potential impact

You'll receive a response within 72 hours. Confirmed critical vulnerabilities should be patched on an expedited basis.

## Threat model highlights

Pollen is a private Android AI layer with optional nearby peer cooperation. The current threat model includes:

### Data protection
- On-device data should remain local by default.
- Cloud upload should never occur without explicit user action.
- Sensitive actions should require consent and clear UX disclosure.

### Peer-to-peer security
- Peer links should use authenticated key exchange, ideally **ECDH**, with encrypted sessions such as **AES-256-GCM**.
- Trust-on-first-use can speed onboarding, but peer fingerprints and trust state should be visible and revocable.
- Nearby/offline operation reduces internet exposure, but does **not** remove local attack risk.

### Main attack classes to address
- malicious nearby peers
- impersonation / Sybil behavior
- replay or relay attacks
- physical proximity attacks
- permission abuse on the local device
- unsafe autonomous actions triggered by misleading input

### Defensive expectations
- Android Keystore-backed identity keys where possible
- peer reputation / admission control for swarm participation
- explicit user approval for sensitive capabilities
- audit logging for autonomous actions
- rate limiting and challenge mechanisms for peer admission

## Meshrabiya note

Meshrabiya is promising as an optional Wi-Fi Direct virtual mesh layer, but it should be treated as a transport module, not a trust boundary.
If enabled, it still needs peer authentication, encryption, and policy checks above the transport layer.
