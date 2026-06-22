# Mistake Notebook V1.2.1 Technical Design And Test Cases

## 1. Scope

This document supports the V1.2.1 Android implementation.

V1.2.1 is a patch release focused on navigation and list interaction stability.

## 2. Technical Changes

- Version updated to `versionCode=4`, `versionName=V1.2.1`.
- Release script outputs `release/mistake-notebook-v1.2.1.apk`.
- `MainActivity` stores the active `ScrollView` reference.
- Library rendering accepts a `scrollY` argument and restores scroll position after selection changes.
- Detail rendering accepts a `scrollY` argument and restores scroll position after selection or subject changes.
- Library top return card no longer shows helper text.
- Library export card removes `选择当前显示错题`.
- Home moves `错题库 / 导出 PDF` into the high-frequency `录入错题` card.

## 3. Test Cases

| ID | Case | Steps | Expected |
| --- | --- | --- | --- |
| HOME-001 | Library entry position | Open home | `错题库 / 导出 PDF` is inside `录入错题` |
| LIB-001 | Top return only | Open library | Top card contains `返回首页` without extra hint text |
| LIB-002 | Export actions | Open library export area | Shows layout selector, `导出 A4 PDF`, and `清空选择`; no `选择当前显示错题` |
| LIB-003 | Selection scroll retention | Scroll down library and select a card | Page stays near the selected card |
| DET-001 | Detail selection scroll retention | Scroll detail and toggle export selection | Page stays near previous position |
| DET-002 | Subject update scroll retention | Scroll detail, update subject | Page stays near previous position |
| BLD-001 | Build APK | `./scripts/build-release.sh` | `release/mistake-notebook-v1.2.1.apk` exists |
| BLD-002 | Verify signature | `apksigner verify --verbose release/mistake-notebook-v1.2.1.apk` | v1/v2/v3 verified |
| BLD-003 | Inspect metadata | `aapt2 dump badging release/mistake-notebook-v1.2.1.apk` | versionCode 4, versionName V1.2.1 |
