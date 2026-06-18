#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SDK_DIR="${ANDROID_HOME:-$ROOT_DIR/.android-sdk}"
BUILD_TOOLS="$SDK_DIR/build-tools/35.0.0"
PLATFORM_JAR="$SDK_DIR/platforms/android-35/android.jar"
OUT_DIR="$ROOT_DIR/build/manual"
APP_DIR="$ROOT_DIR/app/src/main"
RELEASE_DIR="$ROOT_DIR/release"
PACKAGE="com.mistakenotebook.app"

if [[ ! -f "$PLATFORM_JAR" ]]; then
  echo "Missing Android platform jar: $PLATFORM_JAR" >&2
  echo "Install Android SDK platform android-35 first." >&2
  exit 2
fi

for tool in aapt2 d8 zipalign apksigner; do
  if [[ ! -x "$BUILD_TOOLS/$tool" ]]; then
    echo "Missing Android build tool: $BUILD_TOOLS/$tool" >&2
    exit 2
  fi
done

rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR/classes" "$OUT_DIR/dex" "$OUT_DIR/compiled" "$RELEASE_DIR"

"$BUILD_TOOLS/aapt2" compile --dir "$APP_DIR/res" -o "$OUT_DIR/compiled/resources.zip"
"$BUILD_TOOLS/aapt2" link \
  -I "$PLATFORM_JAR" \
  --manifest "$APP_DIR/AndroidManifest.xml" \
  --java "$OUT_DIR/generated" \
  -o "$OUT_DIR/unsigned.apk" \
  "$OUT_DIR/compiled/resources.zip"

find "$APP_DIR/java" "$OUT_DIR/generated" -name '*.java' > "$OUT_DIR/sources.txt"
javac -source 17 -target 17 \
  -classpath "$PLATFORM_JAR" \
  -d "$OUT_DIR/classes" \
  @"$OUT_DIR/sources.txt"

"$BUILD_TOOLS/d8" \
  --lib "$PLATFORM_JAR" \
  --min-api 23 \
  --output "$OUT_DIR/dex" \
  $(find "$OUT_DIR/classes" -name '*.class')

cd "$OUT_DIR/dex"
zip -q "$OUT_DIR/unsigned.apk" classes.dex
cd "$ROOT_DIR"

SIGNING_DIR="$ROOT_DIR/build/signing"
mkdir -p "$SIGNING_DIR"
KEYSTORE="$SIGNING_DIR/debug-release.keystore"
if [[ ! -f "$KEYSTORE" ]]; then
  keytool -genkeypair \
    -keystore "$KEYSTORE" \
    -storepass android \
    -keypass android \
    -alias mistake-notebook \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -dname "CN=Mistake Notebook MVP,O=Local,C=CN"
fi

"$BUILD_TOOLS/zipalign" -f 4 "$OUT_DIR/unsigned.apk" "$OUT_DIR/aligned.apk"
"$BUILD_TOOLS/apksigner" sign \
  --ks "$KEYSTORE" \
  --ks-key-alias mistake-notebook \
  --ks-pass pass:android \
  --key-pass pass:android \
  --v4-signing-enabled false \
  --out "$RELEASE_DIR/mistake-notebook-mvp.apk" \
  "$OUT_DIR/aligned.apk"

"$BUILD_TOOLS/apksigner" verify --verbose "$RELEASE_DIR/mistake-notebook-mvp.apk"
echo "APK created: $RELEASE_DIR/mistake-notebook-mvp.apk"
