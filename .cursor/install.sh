#!/usr/bin/env bash
# Cloud Agent bootstrap for the Kai Kotlin Multiplatform project.
# Idempotent: safe to run repeatedly and against a cached/partial state.
set -euo pipefail

ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/android-sdk}"
CMDLINE_TOOLS_DIR="$ANDROID_SDK_ROOT/cmdline-tools/latest"
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"

# Package versions must track gradle/libs.versions.toml (android-compileSdk / build-tools).
PLATFORM_PACKAGE="platforms;android-37.0"
BUILD_TOOLS_PACKAGE="build-tools;37.0.0"

# 1. Android command-line tools (only downloaded when missing).
if [ ! -x "$CMDLINE_TOOLS_DIR/bin/sdkmanager" ]; then
  echo "Installing Android command-line tools..."
  mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"
  tmp_dir="$(mktemp -d)"
  curl -fsSL -o "$tmp_dir/cmdline-tools.zip" "$CMDLINE_TOOLS_URL"
  unzip -q "$tmp_dir/cmdline-tools.zip" -d "$tmp_dir"
  rm -rf "$CMDLINE_TOOLS_DIR"
  mv "$tmp_dir/cmdline-tools" "$CMDLINE_TOOLS_DIR"
  rm -rf "$tmp_dir"
fi

export ANDROID_SDK_ROOT
export ANDROID_HOME="$ANDROID_SDK_ROOT"
export PATH="$CMDLINE_TOOLS_DIR/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"

# 2. Accept licenses and install the SDK packages Gradle needs (sdkmanager skips
#    already-installed packages, so this is idempotent).
# `yes` receives SIGPIPE once sdkmanager stops reading; tolerate it under pipefail.
yes | sdkmanager --licenses >/dev/null 2>&1 || true
sdkmanager "platform-tools" "$PLATFORM_PACKAGE" "$BUILD_TOOLS_PACKAGE"

# 3. Point Gradle at the SDK (local.properties is git-ignored).
echo "sdk.dir=$ANDROID_SDK_ROOT" > local.properties

# 4. Warm Gradle: downloads the wrapper + dependencies and generates sources
#    (e.g. Version.kt), so the first real build for future agents is fast.
chmod +x gradlew
./gradlew --no-daemon :composeApp:compileKotlinDesktop
