# Mistake Notebook Android V0.1 MVP Plan

## 1. Product Goal

V0.1 is a local-first Android mistake notebook for students.

The app captures or imports a mistake photo, calls Alibaba Bailian Qwen3-VL-Plus to extract clean printable question text, stores the mistake by subject, and exports selected mistakes to A4 PDF.

The key product decision for V0.1 is text-first:

```text
photo/import -> extract clean question text -> confirm subject -> save -> select mistakes -> export A4 PDF
```

Image-level handwriting removal is not the main V0.1 flow. If text extraction fails, the user can still save the original image and export it as an image-based PDF.

## 2. In Scope

- Camera capture.
- Gallery import.
- Local API Key and model setting.
- Default model display: `Qwen3-VL-Plus`.
- API model id: `qwen3-vl-plus`.
- Clean question text extraction through Bailian multimodal API.
- Subject classification with manual override.
- Local mistake library.
- Subject filter in the library.
- Compact mistake cards with selected-state highlighting.
- Detail page for original image and extracted text.
- A4 PDF export:
  - text-first export when extracted text exists;
  - image fallback when extracted text is empty;
  - 2 questions per page;
  - 4 questions per page.
- Share/print handoff through Android system share sheet.

## 3. Out of Scope

- Login and account system.
- Cloud sync.
- Backend service.
- Payment.
- Teacher workflow.
- Full OCR correction editor.
- Dedicated handwriting segmentation model.
- Pixel-level automatic handwriting removal.
- Original/cleaned-image comparison as a primary workflow.

## 4. User Flow

### 4.1 Configure

1. User opens system settings.
2. User enters Bailian API Key.
3. User keeps the default model or enters another compatible model id.
4. User can test the connection.

### 4.2 Capture Or Import

1. User taps camera or gallery import.
2. App stores the original image in app-private storage.
3. App opens the confirmation page.

### 4.3 Extract Question

1. User taps `提取题目`.
2. App compresses the image for model input.
3. App calls Bailian Qwen3-VL-Plus.
4. App shows extraction progress and result.
5. App displays clean question text when available.
6. User can save the text result or save only the original image.

### 4.4 Manage Library

1. User opens the mistake library.
2. User filters by subject.
3. User taps a card to view detail.
4. User selects mistakes for export.
5. Selected cards use a different background and border.

### 4.5 Export PDF

1. User selects mistakes.
2. User chooses 2-per-page or 4-per-page layout.
3. If a mistake has clean text, the PDF uses text.
4. If a mistake has no clean text, the PDF uses the original image.
5. The generated PDF is shared to Android print/share targets.

## 5. Acceptance Criteria

- App installs from `release/mistake-notebook-mvp.apk`.
- App label is `错题本`.
- App has a custom launcher icon.
- Settings can save API Key without plaintext logging.
- `测试连接` calls Bailian and returns a concrete result.
- `提取题目` calls Qwen3-VL-Plus and shows progress.
- Extracted clean question text is visible before saving.
- Saved mistakes persist after app restart.
- Library supports subject filtering.
- Selected cards are visually distinguishable.
- PDF export creates a named PDF file.
- Text-based PDF renders common fractions in worksheet style.
- Image-based PDF preserves original aspect ratio and uses narrow side margins.
