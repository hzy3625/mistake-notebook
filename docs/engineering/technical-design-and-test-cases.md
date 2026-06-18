# Mistake Notebook V0.1 Technical Design And Test Cases

## 1. Scope

This document supports the V0.1 Android implementation.

V0.1 is a native Android app implemented with Java and platform APIs. It is intentionally dependency-light so the release APK can be built from this repository without Gradle network dependency resolution.

The current product flow is text-first:

```text
image input -> Bailian Qwen3-VL-Plus -> clean question text -> local save -> A4 PDF export
```

Automatic image-level handwriting removal is not part of the active V0.1 workflow.

## 2. Runtime Components

| Component | Implementation |
| --- | --- |
| Main UI | `MainActivity` native Android views |
| API key storage | `SecurePrefs` with Android Keystore AES/GCM |
| Image storage | `ImageStore`, app-private files |
| Data storage | `MistakeDatabase`, SQLiteOpenHelper |
| Bailian client | `BailianClient`, OpenAI-compatible DashScope endpoint |
| Text normalization | `CleanTextFormatter` |
| PDF export | `A4PdfExporter`, Android `PdfDocument` |
| File sharing | `SimpleFileProvider` |

## 3. Bailian Integration

Endpoint:

```text
https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
```

Default display name:

```text
Qwen3-VL-Plus
```

Default API model id:

```text
qwen3-vl-plus
```

The request sends one text prompt and one base64 JPEG image. The prompt asks the model to return JSON with:

- `subject`
- `subject_confidence`
- `suggestion`
- `warnings`
- `clean_question_text`

If the model returns plain text instead of JSON, V0.1 treats the content as `clean_question_text`.

## 4. Text Formatting

The app normalizes common model output before display and PDF export:

- remove Markdown fences;
- remove `\(`, `\)`, `\[`, `\]`, and `$`;
- convert `\frac{2}{7}` to `2/7`;
- remove common LaTeX commands such as `\quad`;
- keep question numbers and line breaks.

PDF export detects numeric fractions such as `2/7` and draws numerator, fraction bar, and denominator separately to produce a worksheet-like visual result.

## 5. Data Model

`mistakes` stores:

- `id`
- `subject`
- `original_image_path`
- `processed_image_path`
- `use_processed_image`
- `analysis_json`
- `clean_question_text`
- `created_at`
- `printed`

`processed_image_path` and `use_processed_image` are retained for schema compatibility, but V0.1 export prefers `clean_question_text` and falls back to the original image.

## 6. PDF Layout

Text export:

- A4 page: 595 x 842 points.
- Main margin: 36 points.
- Layout options: 2 or 4 mistakes per page.
- No answer guide lines are drawn; remaining space is blank.

Image fallback export:

- Preserve original image aspect ratio.
- Use a narrower image margin than text export.
- Scale image to use most of the slot while staying inside page bounds.
- Center image in its slot.

## 7. Security

- API Key is never committed.
- API Key is not logged.
- API Key is encrypted with Android Keystore-backed AES/GCM.
- Stored images and database are app-private.
- V0.1 calls Bailian directly from the app; a backend proxy is deferred.

## 8. Test Cases

### 8.1 Settings

| ID | Case | Steps | Expected |
| --- | --- | --- | --- |
| SET-001 | Save API Key | Enter API Key and tap save | Key preview is masked; app does not crash |
| SET-002 | Clear API Key | Tap clear | Key preview returns to unconfigured state |
| SET-003 | Test connection success | Use valid API Key and model | Toast shows Bailian connection success |
| SET-004 | Test connection failure | Use invalid API Key | Toast shows concrete auth or HTTP error |
| SET-005 | Default model | Open settings first time | Model field shows Qwen3-VL-Plus default |

### 8.2 Image Input

| ID | Case | Steps | Expected |
| --- | --- | --- | --- |
| IMG-001 | Import from gallery | Choose image | Confirmation page shows original image |
| IMG-002 | Capture by camera | Take photo | Confirmation page shows captured image |
| IMG-003 | Large image | Import high-resolution image | Original is stored; analysis image is size-limited |

### 8.3 Question Extraction

| ID | Case | Steps | Expected |
| --- | --- | --- | --- |
| EXT-001 | Extract clean text | Tap `提取题目` with valid API Key | Progress card shows steps; clean text appears |
| EXT-002 | JSON response | Mock JSON with `clean_question_text` | Parser reads subject and clean text |
| EXT-003 | Plain text response | Mock non-JSON text | App treats content as clean question text |
| EXT-004 | Model returns LaTeX fraction | Return `\frac{2}{7}` | Display stores normalized `2/7` text |
| EXT-005 | Network failure | Disable network or mock timeout | App shows extraction failure and allows saving original |
| EXT-006 | Missing API Key | Tap extract without Key | App asks user to configure API Key |

### 8.4 Library

| ID | Case | Steps | Expected |
| --- | --- | --- | --- |
| LIB-001 | Save text result | Extract and save | Mistake appears in library as text item |
| LIB-002 | Save original only | Save without extraction | Mistake appears in library as image item |
| LIB-003 | Subject filter | Choose a subject filter | Only matching mistakes are shown |
| LIB-004 | Select card | Tap select | Card background and border change |
| LIB-005 | Detail page | Tap card body | Larger image and clean text are shown |
| LIB-006 | Delete | Delete a mistake | Record disappears from library |

### 8.5 PDF Export

| ID | Case | Steps | Expected |
| --- | --- | --- | --- |
| PDF-001 | Export selected text | Select text mistake and export | PDF uses clean question text |
| PDF-002 | Export selected image | Select image-only mistake and export | PDF uses original image |
| PDF-003 | Fraction rendering | Export text containing `2/7` | PDF draws stacked fraction |
| PDF-004 | Blank space | Export short text | No horizontal answer guide lines appear |
| PDF-005 | Image aspect ratio | Export portrait/landscape images | Image is not stretched |
| PDF-006 | File name | Open share sheet | PDF has a visible display name |
| PDF-007 | Printed mark | Export selected mistakes | Selected records are marked printed |

### 8.6 Build And Release

| ID | Case | Command | Expected |
| --- | --- | --- | --- |
| BLD-001 | Build APK | `./scripts/build-release.sh` | APK exists in `release/` |
| BLD-002 | Verify signature | `apksigner verify --verbose release/mistake-notebook-mvp.apk` | v1/v2/v3 verified |
| BLD-003 | Inspect metadata | `aapt2 dump badging release/mistake-notebook-mvp.apk` | package, version, label, icon present |

## 9. Device Smoke Test

1. Install `release/mistake-notebook-mvp.apk`.
2. Open app and confirm launcher label/icon.
3. Save Bailian API Key in settings.
4. Tap `测试连接`.
5. Import a mistake image.
6. Tap `提取题目`.
7. Confirm progress and clean text display.
8. Save the mistake.
9. Filter library by subject.
10. Select the mistake and export PDF.
11. Open the PDF from the share/print sheet.
