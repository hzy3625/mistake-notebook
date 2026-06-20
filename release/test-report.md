# Mistake Notebook V1.1 Test Report

## Build Artifact

- APK: `release/mistake-notebook-mvp.apk`
- Package: `com.mistakenotebook.app`
- App label: `错题本`
- Version: `V1.1`
- Version code: `2`
- Min SDK: `23`
- Target SDK: `35`

## Implemented Scope

- Custom launcher logo.
- Local settings page for Alibaba Bailian API Key and model.
- API Key encrypted locally with Android Keystore AES/GCM.
- Default model: `Qwen3-VL-Plus` / `qwen3-vl-plus`.
- Camera capture and gallery import.
- Image cropping after capture/import.
- App-private image storage.
- `提取题目` flow through Bailian multimodal API.
- Step-by-step extraction progress display.
- Clean question text display and persistence.
- Local SQLite mistake library ordered by newest first.
- Subject filter in library.
- Compact mistake cards with selected-state highlighting.
- Detail page for image and extracted text.
- A4 PDF export with 1-per-page, 2-per-page, and 4-per-page layout.
- PDF export prefers clean text and falls back to image.
- PDF text renderer draws numeric fractions in worksheet style.
- PDF image fallback preserves aspect ratio and fills page slots with narrow margins.
- PDF share/print handoff through a local read-only content provider.

## Automated / Static Verification

| Check | Result |
| --- | --- |
| Manual Android SDK build script completed | Passed |
| APK generated under `release/` | Passed |
| APK signature verification v1/v2/v3 | Passed |
| Package metadata dump | Passed |
| `versionCode=2` and `versionName=V1.1` present | Passed |
| App label present | Passed |
| App icon present | Passed |
| APK contains `classes.dex` | Passed |
| Crop image UI compiles | Passed |
| 1/2/4 PDF export path compiles | Passed |
| Library query orders by `created_at DESC` | Passed by code review |

## V1.1 Device Smoke Test

ADB device testing requires a connected and authorized Android phone.

Recommended smoke test:

1. Copy and install `release/mistake-notebook-mvp.apk`.
2. Confirm app version `V1.1`, launcher icon, and app label `错题本`.
3. Save Bailian API Key/model in settings.
4. Tap `测试连接`.
5. Import or capture a mistake image.
6. Tap `裁截图片`, drag the crop frame, and save crop.
7. Tap `提取题目`.
8. Confirm progress and extracted text.
9. Save the mistake.
10. Add another mistake and confirm newest-first order.
11. Filter by subject in library.
12. Select one or more mistakes.
13. Export A4 PDF with 1/2/4 questions per page and open the share/print sheet.
