# Mistake Notebook V1.2.1 Test Report

## Build Artifact

- APK: `release/mistake-notebook-v1.2.1.apk`
- Package: `com.mistakenotebook.app`
- App label: `错题本`
- Version: `V1.2.1`
- Version code: `4`
- Min SDK: `23`
- Target SDK: `35`

## Implemented Scope

- Home `错题库 / 导出 PDF` moved into `录入错题`.
- Library `返回首页` card no longer shows extra hint text.
- Library export area removed `选择当前显示错题`.
- Library selection preserves scroll position after card selection changes.
- Detail page preserves scroll position after export-selection changes.
- Detail page preserves scroll position after subject updates.

## Verification

| Check | Result |
| --- | --- |
| Manual Android SDK build script completed | Passed |
| Versioned APK generated under `release/` | Passed |
| APK signature verification v1/v2/v3 | Passed |
| Package metadata dump | Passed |
| `versionCode=4` and `versionName=V1.2.1` present | Passed |
| Scroll-retention code compiles | Passed |
| Removed obsolete library actions | Passed |

## Device Smoke Test

1. Install `release/mistake-notebook-v1.2.1.apk`.
2. Confirm home `错题库 / 导出 PDF` appears under `录入错题`.
3. Open library and confirm `返回首页` is near top without hint text.
4. Confirm export area has one primary export button and no `选择当前显示错题`.
5. Scroll down a long library and select a card; confirm scroll position is retained.
6. Open detail, scroll down, toggle selection and update subject; confirm scroll position is retained.
