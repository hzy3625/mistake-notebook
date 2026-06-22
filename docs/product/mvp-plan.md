# Mistake Notebook Android V1.2 Plan

## 1. Product Goal

V1.2 is a local-first Android mistake notebook for students.

The app captures or imports a mistake photo, optionally crops the question area, calls Alibaba Bailian Qwen3-VL-Plus to extract clean printable question text, stores the mistake by subject, supports backup migration between phones, and exports selected mistakes to A4 PDF.

## 2. V1.2 Scope

- Home page is organized by operation frequency:
  - `录入错题`
  - `导出 / 导入`
  - `百炼配置`
- Mistake library keeps records in reverse chronological order.
- Library page puts `返回首页` near the top.
- PDF export uses one primary action with a layout selector; default is 2 questions per page.
- PDF export supports 1, 2, or 4 questions per page.
- Mistake detail page can update subject classification.
- Data backup export/import supports migration between phones.
- Release APK filename includes version, for example `mistake-notebook-v1.2.apk`.

## 3. In Scope

- Camera capture.
- Gallery import.
- Post-capture/post-import image crop.
- Local API Key and model setting.
- Clean question text extraction through Bailian multimodal API.
- Subject classification with manual override.
- Subject update from detail page.
- Local mistake library.
- Newest-first library order.
- Subject filter in the library.
- Select all visible mistakes and clear selection.
- Backup export as a zip file.
- Backup import from another phone.
- A4 PDF export:
  - text-first export when extracted text exists;
  - image fallback when extracted text is empty;
  - 1 question per page;
  - 2 questions per page by default;
  - 4 questions per page.

## 4. Out of Scope

- Login and account system.
- Cloud sync.
- Backend service.
- Payment.
- Full OCR correction editor.
- Dedicated handwriting segmentation model.
- Pixel-level automatic handwriting removal.

## 5. User Flow

### 5.1 Capture And Save

1. User taps camera or gallery import.
2. App stores the image in app-private storage.
3. User optionally crops the image.
4. User optionally taps `提取题目`.
5. User confirms subject and saves.

### 5.2 Manage Library

1. User opens the library from the home page.
2. `返回首页` is available at the top of the library.
3. Newest mistakes appear first.
4. User filters by subject.
5. User selects one mistake, all visible mistakes, or clears selection.
6. User taps a card to view detail.
7. User can update the subject classification in detail.

### 5.3 Export PDF

1. User selects mistakes.
2. User keeps default `每页 2 题` or switches to `每页 1 题` / `每页 4 题`.
3. User taps one primary `导出 A4 PDF` button.
4. The generated PDF is shared to Android print/share targets.

### 5.4 Backup Migration

1. On phone A, user taps `导出数据备份`.
2. App creates a zip backup containing metadata, images, and extracted text.
3. User transfers the zip to phone B.
4. On phone B, user taps `导入数据备份`.
5. App restores mistakes into local storage and database.

## 6. Acceptance Criteria

- App installs from `release/mistake-notebook-v1.2.apk`.
- App version is `V1.2`.
- Library `返回首页` is visible near the top.
- Library export uses one primary button and a layout selector.
- Default PDF layout is `每页 2 题`.
- Detail page can update subject classification.
- Backup export creates a shareable zip file.
- Backup import restores mistakes on another phone.
- PDF export supports 1/2/4 questions per page.
- Image-based PDF preserves aspect ratio and uses narrow margins.
