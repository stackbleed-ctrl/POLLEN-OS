#!/usr/bin/env bash
set -euo pipefail

if ! command -v gradle >/dev/null 2>&1; then
  echo "Gradle is not installed."
  echo "Install Gradle or use Android Studio, then run: gradle wrapper"
  exit 1
fi

mkdir -p gradle/wrapper
if [ -f gradle-wrapper.properties ] && [ ! -f gradle/wrapper/gradle-wrapper.properties ]; then
  cp gradle-wrapper.properties gradle/wrapper/gradle-wrapper.properties
fi

gradle wrapper
echo "Gradle wrapper generated."
