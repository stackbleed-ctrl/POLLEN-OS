# Install

## Requirements

- Android Studio Hedgehog or newer
- Android 10+ target devices
- Two devices recommended for offline swarm testing

## Open and run

1. Open this repo in Android Studio.
2. Let Gradle sync.
3. Build and run the debug variant on Android 10+.
4. Start with one device, then add a second device for Nearby tests.

## Permissions

Permissions are grouped by purpose so testing is less intimidating.

### Core runtime
- foreground service
- notifications / notification listener access

### Communication
- nearby devices
- Bluetooth / local radios as required by the device and Android version

### Optional assistant actions
- SMS
- call screening / call state
- camera

All permissions should be **runtime-requested where applicable**, and autonomous features should require **explicit user consent** before activation.
In production, these should also be selectively disableable from app settings.

## Enable system access

1. Grant notification permission if prompted.
2. Manually enable notification access for Pollen in system settings.
3. On supported devices, set Pollen as the default call screening app if you want to test call events.
4. For swarm testing, install on a second device and keep Nearby/Bluetooth/Wi-Fi radios enabled as needed.

## Demo goals

Start with:
- `summarize my notifications`
- `open maps`

Then test stronger product demos:
- `Check the weather and send a summary via SMS to my partner if it's raining`
- `Use the camera on the nearest device with good lighting to describe what's in front of me`

## Local model planning note

Expect roughly:
- **~1.3 GB** model download for a 2B-class int4 package
- **~2–3 GB RAM** during active inference on capable devices

Validate exact performance on your target phones before making public claims.
