# Launch checklist

## Before publishing repo
- [ ] choose final repo name
- [ ] set public SourceCode URLs in metadata/docs
- [ ] review license choice
- [ ] replace placeholder signing key metadata
- [ ] verify package name and app label

## Before first tester release
- [ ] build debug APK locally
- [ ] test on at least 2 devices
- [ ] verify notification listener flow
- [ ] verify foreground service persistence
- [ ] verify no unexpected network traffic
- [ ] capture screenshots / short demo video

## Feedback collection
- [ ] enable GitHub Issues
- [ ] pin feedback request issue
- [ ] ask testers for device model + Android version
- [ ] ask testers for battery impact notes
- [ ] ask testers which permissions felt uncomfortable

## Security sanity
- [ ] verify consent before sensitive actions
- [ ] verify peer auth plan before enabling nearby control
- [ ] verify local-only behavior claims
