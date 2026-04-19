#!/usr/bin/env bash
set -euo pipefail

# Moves the flat repo into a more standard Android structure.
# Safe to run once on a branch. Review the result before merging.

mkdir -p app/src/main/java
mkdir -p app/src/test/java
mkdir -p app/src/androidTest/java
mkdir -p app/src/main/res/values
mkdir -p app/src/main/res/xml
mkdir -p app/src/main/res/drawable

# Move Android resources if present
[ -f AndroidManifest.xml ] && mv AndroidManifest.xml app/src/main/AndroidManifest.xml
[ -f strings.xml ] && mv strings.xml app/src/main/res/values/strings.xml
[ -f colors.xml ] && mv colors.xml app/src/main/res/values/colors.xml
[ -f themes.xml ] && mv themes.xml app/src/main/res/values/themes.xml
[ -f backup_rules.xml ] && mv backup_rules.xml app/src/main/res/xml/backup_rules.xml
[ -f data_extraction_rules.xml ] && mv data_extraction_rules.xml app/src/main/res/xml/data_extraction_rules.xml
[ -f ic_notification.xml ] && mv ic_notification.xml app/src/main/res/drawable/ic_notification.xml
[ -f ic_pollen_logo.xml ] && mv ic_pollen_logo.xml app/src/main/res/drawable/ic_pollen_logo.xml
[ -f proguard-rules.pro ] && mv proguard-rules.pro app/proguard-rules.pro

# Main Kotlin files
mkdir -p app/src/main/java/com/stackbleedctrl/pollyn
mkdir -p app/src/main/java/com/stackbleedctrl/pollyn/oslayer
mkdir -p app/src/main/java/com/stackbleedctrl/pollyn/ui
mkdir -p app/src/main/java/com/stackbleedctrl/pollyn/ui/theme

# This repo has package declarations already; these moves are best-effort.
for f in MainActivity.kt MainViewModel.kt PollynApp.kt BootReceiver.kt AppModule.kt; do
  [ -f "$f" ] && mv "$f" app/src/main/java/com/stackbleedctrl/pollyn/"$f"
done

for f in PollynBrain.kt PollynBrainService.kt PollynCallScreeningService.kt PollynNotificationListenerService.kt ActionExecutor.kt BrainDecision.kt BrainEventBus.kt CrdtMemoryStore.kt GeminiNanoAdapter.kt HybridLogicalClock.kt LlmBackend.kt LocalLlmManager.kt NearbyMeshCoordinator.kt NodeTrustManager.kt PeerNode.kt PhoneEvent.kt PollynSdk.kt PollynTracer.kt RuleBasedFallbackLlm.kt SmartRoutingTable.kt SwarmCoordinator.kt; do
  [ -f "$f" ] && mv "$f" app/src/main/java/com/stackbleedctrl/pollyn/oslayer/"$f"
done

for f in PollynDashboardScreen.kt PollynUiState.kt Theme.kt; do
  [ -f "$f" ] && mv "$f" app/src/main/java/com/stackbleedctrl/pollyn/ui/"$f"
done

# Tests
for f in BrainEventBusTest.kt IntentRouterTest.kt RuleBasedFallbackLlmTest.kt; do
  [ -f "$f" ] && mv "$f" app/src/test/java/"$f"
done

[ -f ExampleInstrumentedTest.kt ] && mv ExampleInstrumentedTest.kt app/src/androidTest/java/ExampleInstrumentedTest.kt

echo "Restructure complete. Review git diff before committing."
