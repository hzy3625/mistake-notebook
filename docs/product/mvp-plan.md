# Mistake Notebook Android V1.1 Plan

## 1. Product Goal

V1.1 is a local-first Android mistake notebook for students.

The app captures or imports a mistake photo, optionally crops the question area, calls Alibaba Bailian Qwen3-VL-Plus to extract clean printable question text, stores the mistake by subject, and exports selected mistakes to A4 PDF.

The product flow is:

```text
photo/import -> crop -> extract clean question text -> confirm subject -> save -> select mistakes -> export A4 PDF
```

Image-level handwriting removal is not the V1.1 workflow. If text extraction fails, the user can still save the cropped/original image and export it as an image-based PDF.

## 2. V1.1 Scope

- Mistake library displays records in reverse chronological order.
- After camera capture or gallery import, user can crop the image before extraction or saving.
- A4 PDF export supports 1, 2, or 4 questions per page.
- Image-only PDF export scales images to fill the available page slot as much as possible while preserving aspect ratio.

## 3. In Scope

- Camera capture.
- Gallery import.
- Post-capture/post-import image crop.
- Local API Key and model setting.
- Default model display: `Qwen3-VL-Plus`.
- API model id: `qwen3-vl-plus`.
- Clean question text extraction through Bailian multimodal API.
- Subject classification with manual override.
- Local mistake library.
- Newest-first library order.
- Subject filter in the library.
- Compact mistake cards with selected-state highlighting.
- Detail page for original/cropped image and extracted text.
- A4 PDF export:
  - text-first export when extracted text exists;
  - image fallback when extracted text is empty;
  - 1 question per page;
  - 2 questions per page;
  - 4 questions per page.
- Share/print handoff through Android system share sheet.

## 4. Out of Scope

- Login and account system.
- Cloud sync.
- Backend service.
- Payment.
- Teacher workflow.
- Full OCR correction editor.
- Dedicated handwriting segmentation model.
- Pixel-level automatic handwriting removal.
- Original/cleaned-image comparison as a primary workflow.

## 5. User Flow

### 5.1 Configure

1. User opens system settings.
2. User enters Bailian API Key.
3. User keeps the default model or enters another compatible model id.
4. User can test the connection.

### 5.2 Capture Or Import

1. User taps camera or gallery import.
2. App stores the image in app-private storage.
3. App opens the confirmation page.

### 5.3 Crop And Extract Question

1. User optionally taps `裁截图片`.
2. User drags the crop frame to keep the question area.
3. User saves the cropped image.
4. User taps `提取题目`.
5. App compresses the image for model input.
6. App calls Bailian Qwen3-VL-Plus.
7. App shows extraction progress and result.
8. App displays clean question text when available.
9. User can save the text result or save only the image.

### 5.4 Manage Library

1. User opens the mistake library.
2. Newest mistakes appear first.
3. User filters by subject.
4. User taps a card to view detail.
5. User selects mistakes for export.
6. Selected cards use a different background and border.

### 5.5 Export PDF

1. User selects mistakes.
2. User chooses 1-per-page, 2-per-page, or 4-per-page layout.
3. If a mistake has clean text, the PDF uses text.
4. If a mistake has no clean text, the PDF uses the image.
5. Image fallback preserves aspect ratio and fills the slot as much as possible.
6. The generated PDF is shared to Android print/share targets.

## 6. Acceptance Criteria

- App installs from `release/mistake-notebook-mvp.apk`.
- App label is `错题本`.
- App version is `V1.1`.
- App has a custom launcher icon.
- Settings can save API Key without plaintext logging.
- `测试连接` calls Bailian and returns a concrete result.
- Confirmation page supports `裁截图片`.
- `提取题目` calls Qwen3-VL-Plus and shows progress.
- Extracted clean question text is visible before saving.
- Saved mistakes persist after app restart.
- Library displays newest mistakes first.
- Library supports subject filtering.
- Selected cards are visually distinguishable.
- PDF export creates a named PDF file.
- PDF export supports 1/2/4 questions per page.
- Text-based PDF renders common fractions in worksheet style.
- Image-based PDF preserves original aspect ratio and uses narrow margins.
