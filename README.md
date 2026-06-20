# Mistake Notebook

Android V1.1 mistake notebook MVP.

The app lets students capture or import mistake photos, extract clean printable question text with Alibaba Bailian Qwen3-VL-Plus, manage mistakes by subject, and export selected mistakes to A4 PDF.

## Release

Installable APK:

```text
release/mistake-notebook-mvp.apk
```

Current version:

```text
V1.1
```

## Build

```bash
./scripts/build-release.sh
```

The build script uses the local Android SDK at `.android-sdk` by default, or `ANDROID_HOME` when set.
