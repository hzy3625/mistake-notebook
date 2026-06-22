# Mistake Notebook V1.2 Technical Design And Test Cases

## 1. Scope

This document supports the V1.2 Android implementation.

V1.2 keeps the native Java/platform-API architecture and adds library interaction improvements, subject updates, backup migration, and versioned release artifacts.

## 2. Runtime Components

| Component | Implementation |
| --- | --- |
| Main UI | `MainActivity` native Android views |
| API key storage | `SecurePrefs` with Android Keystore AES/GCM |
| Image storage | `ImageStore`, app-private files |
| Image crop | `CropImageView` |
| Data storage | `MistakeDatabase`, SQLiteOpenHelper |
| Backup export/import | `MistakeBackupStore`, zip + TSV manifest |
| Bailian client | `BailianClient`, OpenAI-compatible DashScope endpoint |
| Text normalization | `CleanTextFormatter` |
| PDF export | `A4PdfExporter`, Android `PdfDocument` |
| File sharing | `SimpleFileProvider` |

## 3. V1.2 Technical Changes

- Version updated to `versionCode=3`, `versionName=V1.2`.
- Release script outputs `release/mistake-notebook-v1.2.apk`.
- Home cards are ordered by operation frequency.
- Library top area contains `返回首页`, filter, export layout selector, and selection controls.
- PDF export default is `perPage=2`; `perPage=1` and `perPage=4` are selectable.
- `MistakeDatabase.updateSubject()` updates subject classification from detail page.
- `MistakeBackupStore` exports and imports backup zip files.
- `ImageStore.saveBytes()` restores image bytes during backup import.

## 4. Backup Format

Backup file:

```text
mistake-notebook-backup-yyyyMMdd-HHmmss.zip
```

Zip entries:

- `mistakes.tsv`: manifest.
- `images/original/*.jpg`: original/cropped mistake images.
- `images/processed/*.jpg`: reserved for processed images.

Manifest columns:

```text
m subject originalEntry processedEntry useProcessed createdAt printed analysisBase64 cleanTextBase64
```

The backup intentionally does not include Bailian API Key or Android Keystore data.

## 5. PDF Layout

- A4 page: 595 x 842 points.
- Export options: 1, 2, or 4 questions per page.
- Default: 2 questions per page.
- Text export uses extracted clean text first.
- Image export preserves aspect ratio and scales to fill the slot with narrow margins.

## 6. Test Cases

### 6.1 Home And Navigation

| ID | Case | Steps | Expected |
| --- | --- | --- | --- |
| HOME-001 | Home order | Open app | Cards appear as record, export/import, Bailian config |
| LIB-001 | Top return | Open library with many mistakes | `返回首页` is available near top |
| LIB-002 | Newest first | Save multiple mistakes | Newest appears first |

### 6.2 Library Export UI

| ID | Case | Steps | Expected |
| --- | --- | --- | --- |
| EXP-001 | Default layout | Open library | Export selector defaults to `每页 2 题` |
| EXP-002 | One primary export | Open library | Only one primary `导出 A4 PDF` action is shown |
| EXP-003 | Switch layout | Select `每页 1 题` or `每页 4 题` | Export uses selected layout |
| EXP-004 | Select visible | Tap `选择当前显示错题` | Visible mistake cards become selected |
| EXP-005 | Clear selection | Tap `清空选择` | Selected count becomes 0 |

### 6.3 Detail Subject Update

| ID | Case | Steps | Expected |
| --- | --- | --- | --- |
| SUB-001 | Update subject | Open detail, choose subject, save | Detail shows updated subject |
| SUB-002 | Filter after update | Update subject, return library, filter | Mistake appears under new subject |

### 6.4 Backup

| ID | Case | Steps | Expected |
| --- | --- | --- | --- |
| BAK-001 | Export backup | Tap `导出数据备份` | Share sheet opens with zip backup |
| BAK-002 | Import backup | Select a valid backup zip | Mistakes are inserted into library |
| BAK-003 | Missing manifest | Import invalid zip | User sees import failure |
| BAK-004 | Security | Export backup | API Key is not included |

### 6.5 Build And Release

| ID | Case | Command | Expected |
| --- | --- | --- | --- |
| BLD-001 | Build APK | `./scripts/build-release.sh` | `release/mistake-notebook-v1.2.apk` exists |
| BLD-002 | Verify signature | `apksigner verify --verbose release/mistake-notebook-v1.2.apk` | v1/v2/v3 verified |
| BLD-003 | Inspect metadata | `aapt2 dump badging release/mistake-notebook-v1.2.apk` | versionCode 3, versionName V1.2 |
