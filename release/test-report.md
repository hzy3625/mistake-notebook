# Mistake Notebook V1.2 Test Report

## Build Artifact

- APK: `release/mistake-notebook-v1.2.apk`
- Package: `com.mistakenotebook.app`
- App label: `错题本`
- Version: `V1.2`
- Version code: `3`
- Min SDK: `23`
- Target SDK: `35`

## Implemented Scope

- Home page reordered by operation frequency.
- Library top area now contains `返回首页`.
- Library export actions merged into one primary `导出 A4 PDF` button with a layout selector.
- Export default layout is `每页 2 题`.
- Export still supports `每页 1 题`, `每页 2 题`, and `每页 4 题`.
- Detail page supports subject classification update.
- Data backup export/import supports moving mistakes between phones.
- Release APK filename includes version.

## Verification

| Check | Result |
| --- | --- |
| Manual Android SDK build script completed | Passed |
| Versioned APK generated under `release/` | Passed |
| APK signature verification v1/v2/v3 | Passed |
| Package metadata dump | Passed |
| `versionCode=3` and `versionName=V1.2` present | Passed |
| Backup export/import code compiles | Passed |
| Subject update code compiles | Passed |
| One-button PDF export code compiles | Passed |

## Device Smoke Test

1. Install `release/mistake-notebook-v1.2.apk`.
2. Confirm app version `V1.2`.
3. Confirm home order: record, export/import, Bailian config.
4. Open library and confirm `返回首页` near top.
5. Select mistakes, keep default `每页 2 题`, export PDF.
6. Switch to `每页 1 题` and `每页 4 题`, export again.
7. Open detail and update subject.
8. Export backup zip.
9. Import backup zip on another phone or fresh install.
