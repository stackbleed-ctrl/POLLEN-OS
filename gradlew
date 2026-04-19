#!/usr/bin/env sh

#
# Minimal Gradle bootstrap script.
# This script expects gradle/wrapper/gradle-wrapper.jar to exist after `gradle wrapper`.
#

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$CLASSPATH" ]; then
  echo "Missing gradle-wrapper.jar."
  echo "Run: gradle wrapper"
  exit 1
fi

exec java -Dorg.gradle.appname=gradlew -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
