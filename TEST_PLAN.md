# Device Test Plan

## Device 1
- install debug build
- grant notification access and core runtime permissions
- start service
- trigger notification events

## Device 2
- install same build
- start service
- keep nearby radios enabled
- enable optional camera permission if testing delegated vision tasks

## Expected
- both devices advertise/discover peers
- dashboard shows local state changes
- service remains foreground sticky
- local fallback LLM produces decisions even offline
- optional delegated tasks prefer the more capable nearby device when routing is enabled

## Manual checks
- send a notification from a messaging app and confirm listener logs it
- enter `summarize my notifications`
- enter `open maps`
- enter `Check the weather and send a summary via SMS to my partner if it's raining`
- enter `Use the camera on the nearest device with good lighting to describe what's in front of me`
