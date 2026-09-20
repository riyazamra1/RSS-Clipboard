#!/bin/sh
set -eu
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$JAR" ]; then
  mkdir -p "$APP_HOME/gradle/wrapper"
  command -v curl >/dev/null 2>&1 || { echo "curl is required to bootstrap Gradle Wrapper" >&2; exit 1; }
  curl -fsSL "https://raw.githubusercontent.com/gradle/gradle/v8.13.0/gradle/wrapper/gradle-wrapper.jar" -o "$JAR"
fi
exec java -classpath "$JAR" org.gradle.wrapper.GradleWrapperMain "$@"
