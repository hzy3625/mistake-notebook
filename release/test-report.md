# Mistake Notebook V0.1 Test Report

## Build Artifact

- APK: `release/mistake-notebook-mvp.apk`
- Package: `com.mistakenotebook.app`
- App label: `错题本`
- Version: `V0.1`
- Version code: `1`
- Min SDK: `23`
- Target SDK: `35`

## Implemented Scope

- Custom launcher logo.
- Local settings page for Alibaba Bailian API Key and model.
- API Key encrypted locally with Android Keystore AES/GCM.
- Default model: `Qwen3-VL-Plus` / `qwen3-vl-plus`.
- Camera capture and gallery import.
- App-private original image storage.
- `提取题目` flow through Bailian multimodal API.
- Step-by-step extraction progress display.
- Clean question text display and persistence.
- Local SQLite mistake library.
- Subject filter in library.
- Compact mistake cards with selected-state highlighting.
- Detail page for original image and extracted text.
- A4 PDF export with 2-per-page and 4-per-page layout.
- PDF export prefers clean text and falls back to original image.
- PDF text renderer draws numeric fractions in worksheet style.
- PDF image fallback preserves aspect ratio and uses narrow side margins.
- PDF share/print handoff through a local read-only content provider.

## Automated / Static Verification

| Check | Result |
| --- | --- |
| Manual Android SDK build script completed | Passed |
| APK generated under `release/` | Passed |
| APK signature verification v1 | Passed |
| APK signature verification v2 | Passed |
| APK signature verification v3 | Passed |
| Package metadata dump | Passed |
| `versionCode` and `versionName` present | Passed |
| `minSdkVersion` and `targetSdkVersion` present | Passed |
| App label present | Passed |
| App icon present | Passed |
| APK contains `classes.dex` | Passed |
| Text extraction UI compiles | Passed |
| Subject filter UI compiles | Passed |
| PDF export path compiles | Passed |

## Device Feedback Fixes Included

| Issue | Fix |
| --- | --- |
| API Key save failed with AES/GCM IV error | Let `Cipher` generate IV and store `cipher.getIV()` |
| Test connection was local-only | `测试连接` now performs a real Bailian request |
| Share sheet showed `No name` | File provider returns display name and size; share intent sets title/subject/ClipData |
| Model output contained LaTeX source | `CleanTextFormatter` normalizes common LaTeX wrappers and fractions |
| PDF fractions looked like raw text | PDF renderer draws numeric fractions as stacked numerator/denominator |
| PDF had answer guide lines | Guide lines removed; blank space is retained |
| Image-only PDF had excessive side whitespace | Image fallback preserves aspect ratio with narrow side margins |
| Library cards were too large and hard to scan | Cards are compact, with detail view for large image |
| Selected mistakes were hard to distinguish | Selected cards use highlighted background and border |
| UI wording still said analysis | Main action is now `提取题目` |

## Commands To Run

```bash
./scripts/build-release.sh
.android-sdk/build-tools/35.0.0/apksigner verify --verbose release/mistake-notebook-mvp.apk
.android-sdk/build-tools/35.0.0/aapt2 dump badging release/mistake-notebook-mvp.apk
```

## Device Smoke Test

ADB device testing requires a connected and authorized Android phone.

Recommended smoke test:

1. Copy and install `release/mistake-notebook-mvp.apk`.
2. Confirm launcher icon and app label `错题本`.
3. Save Bailian API Key/model in settings.
4. Tap `测试连接`.
5. Import or capture a mistake image.
6. Tap `提取题目`.
7. Confirm progress and extracted text.
8. Save the mistake.
9. Filter by subject in library.
10. Select one or more mistakes.
11. Export A4 PDF and open the share/print sheet.
