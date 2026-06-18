#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ADB="${ANDROID_HOME:-$ROOT_DIR/.android-sdk}/platform-tools/adb"
APK="$ROOT_DIR/release/mistake-notebook-mvp.apk"
PACKAGE="com.mistakenotebook.app"

if [[ ! -x "$ADB" ]]; then
  echo "Missing adb: $ADB" >&2
  exit 2
fi

if [[ ! -f "$APK" ]]; then
  echo "Missing APK: $APK" >&2
  exit 2
fi

"$ADB" start-server >/dev/null
DEVICE_COUNT="$("$ADB" devices | awk 'NR > 1 && $2 == "device" { count++ } END { print count + 0 }')"
if [[ "$DEVICE_COUNT" -eq 0 ]]; then
  echo "No authorized Android device found. Enable USB debugging and authorize this computer." >&2
  "$ADB" devices -l
  exit 3
fi

echo "Installing $APK"
"$ADB" install -r "$APK"

echo "Launching $PACKAGE"
"$ADB" shell monkey -p "$PACKAGE" -c android.intent.category.LAUNCHER 1 >/dev/null

echo "Collecting recent app logcat lines"
"$ADB" logcat -d -t 300 | grep "$PACKAGE" || true

echo "Device smoke test completed. Please verify settings save, Bailian test connection, image import, and PDF export on screen."

