# Mistake Notebook V1.1 Technical Design And Test Cases

## 1. Scope

This document supports the V1.1 Android implementation.

V1.1 is a native Android app implemented with Java and platform APIs. It is dependency-light so the release APK can be built from this repository without Gradle network dependency resolution.

The current product flow is:

```text
image input -> crop -> Bailian Qwen3-VL-Plus -> clean question text -> local save -> A4 PDF export
```

Automatic image-level handwriting removal is not part of the active V1.1 workflow.

## 2. Runtime Components

| Component | Implementation |
| --- | --- |
| Main UI | `MainActivity` native Android views |
| API key storage | `SecurePrefs` with Android Keystore AES/GCM |
| Image storage | `ImageStore`, app-private files |
| Image crop | `CropImageView` |
| Data storage | `MistakeDatabase`, SQLiteOpenHelper |
| Bailian client | `BailianClient`, OpenAI-compatible DashScope endpoint |
| Text normalization | `CleanTextFormatter` |
| PDF export | `A4PdfExporter`, Android `PdfDocument` |
| File sharing | `SimpleFileProvider` |

## 3. V1.1 Technical Changes

- `MistakeDatabase.listAll()` orders records by `created_at DESC`.
- `CropImageView` provides rectangular crop with drag and corner resize handles.
- Cropping saves a new JPEG into app-private original image storage and resets extraction state.
- `A4PdfExporter` accepts `perPage = 1`, `2`, or `4`.
- Image fallback export uses narrow margins and centers the largest aspect-ratio-preserving image inside each slot.
- Manifest/build version updated to `versionCode=2`, `versionName=V1.1`.

## 4. Bailian Integration

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

If the model returns plain text instead of JSON, V1.1 treats the content as `clean_question_text`.

## 5. Crop Design

`CropImageView` is a custom view with:

- fit-center bitmap rendering;
- movable crop rectangle;
- draggable corner handles;
- dimmed area outside crop rectangle;
- conversion from view coordinates to bitmap coordinates;
- JPEG save back into app-private original image storage.

The crop view calls `requestDisallowInterceptTouchEvent(true)` while dragging so parent scrolling does not steal crop gestures.

## 6. PDF Layout

Text export:

- A4 page: 595 x 842 points.
- Text margin: 36 points.
- Layout options: 1, 2, or 4 mistakes per page.
- No answer guide lines are drawn; remaining space is blank.

Image fallback export:

- Preserve original image aspect ratio.
- Use 8pt page/slot image margin.
- Scale image to use most of the slot while staying inside page bounds.
- Center image in its slot.

## 7. Data Model

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

`processed_image_path` and `use_processed_image` are retained for schema compatibility, but V1.1 export prefers `clean_question_text` and falls back to the image.

## 8. Test Cases

### 8.1 Settings

| ID | Case | Steps | Expected |
| --- | --- | --- | --- |
| SET-001 | Save API Key | Enter API Key and tap save | Key preview is masked; app does not crash |
| SET-002 | Clear API Key | Tap clear | Key preview returns to unconfigured state |
| SET-003 | Test connection success | Use valid API Key and model | Toast shows Bailian connection success |
| SET-004 | Test connection failure | Use invalid API Key | Toast shows concrete auth or HTTP error |
| SET-005 | Default model | Open settings first time | Model field shows Qwen3-VL-Plus default |

### 8.2 Image Input And Crop

| ID | Case | Steps | Expected |
| --- | --- | --- | --- |
| IMG-001 | Import from gallery | Choose image | Confirmation page shows image |
| IMG-002 | Capture by camera | Take photo | Confirmation page shows captured image |
| IMG-003 | Large image | Import high-resolution image | Image is stored and size-limited |
| IMG-004 | Crop after input | Tap `裁截图片`, drag frame, save | Confirmation page shows cropped image |
| IMG-005 | Crop bounds | Drag frame/corners outside image | Crop frame remains inside image bounds |

### 8.3 Question Extraction

| ID | Case | Steps | Expected |
| --- | --- | --- | --- |
| EXT-001 | Extract clean text | Tap `提取题目` with valid API Key | Progress card shows steps; clean text appears |
| EXT-002 | JSON response | Mock JSON with `clean_question_text` | Parser reads subject and clean text |
| EXT-003 | Plain text response | Mock non-JSON text | App treats content as clean question text |
| EXT-004 | Model returns LaTeX fraction | Return `\frac{2}{7}` | Display stores normalized `2/7` text |
| EXT-005 | Network failure | Disable network or mock timeout | App shows extraction failure and allows saving image |
| EXT-006 | Missing API Key | Tap extract without Key | App asks user to configure API Key |

### 8.4 Library

| ID | Case | Steps | Expected |
| --- | --- | --- | --- |
| LIB-001 | Save text result | Extract and save | Mistake appears in library as text item |
| LIB-002 | Save image only | Save without extraction | Mistake appears in library as image item |
| LIB-003 | Newest first | Save multiple mistakes | Newest mistake appears first |
| LIB-004 | Subject filter | Choose a subject filter | Only matching mistakes are shown |
| LIB-005 | Select card | Tap select | Card background and border change |
| LIB-006 | Detail page | Tap card body | Larger image and clean text are shown |
| LIB-007 | Delete | Delete a mistake | Record disappears from library |

### 8.5 PDF Export

| ID | Case | Steps | Expected |
| --- | --- | --- | --- |
| PDF-001 | Export selected text | Select text mistake and export | PDF uses clean question text |
| PDF-002 | Export selected image | Select image-only mistake and export | PDF uses image |
| PDF-003 | One per page | Export with `每页 1 题` | Each mistake gets a full page slot |
| PDF-004 | Two per page | Export with `每页 2 题` | Page has up to two slots |
| PDF-005 | Four per page | Export with `每页 4 题` | Page has up to four slots |
| PDF-006 | Fraction rendering | Export text containing `2/7` | PDF draws stacked fraction |
| PDF-007 | Image aspect ratio | Export portrait/landscape images | Image is not stretched |
| PDF-008 | Image fills slot | Export image-only item | Image is scaled close to slot bounds |
| PDF-009 | File name | Open share sheet | PDF has a visible display name |
| PDF-010 | Printed mark | Export selected mistakes | Selected records are marked printed |

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
5. Import or capture a mistake image.
6. Tap `裁截图片`, drag the crop frame, and save.
7. Tap `提取题目`.
8. Confirm progress and clean text display.
9. Save the mistake.
10. Confirm newest-first order in library.
11. Filter library by subject.
12. Select the mistake and export PDF with 1/2/4 questions per page.
13. Open the PDF from the share/print sheet.
