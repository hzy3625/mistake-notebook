# Mistake Notebook Android V1.2.1 Plan

## 1. Product Goal

V1.2.1 is a usability-focused patch release for the local-first Android mistake notebook.

The app keeps the V1.2 backup, subject update, crop, and PDF export capabilities, while refining high-frequency library interactions.

## 2. V1.2.1 Scope

- Move `错题库 / 导出 PDF` back into the home `录入错题` card.
- Keep home order by operation frequency:
  - `录入错题`
  - `导出 / 导入`
  - `百炼配置`
- Remove the extra hint under library `返回首页`.
- Remove `选择当前显示错题`.
- Keep one primary `导出 A4 PDF` button with layout selector.
- Preserve scroll position when selecting mistakes in the library.
- Preserve scroll position when changing selection or subject in mistake detail.
- Release APK filename includes patch version: `mistake-notebook-v1.2.1.apk`.

## 3. Acceptance Criteria

- App installs from `release/mistake-notebook-v1.2.1.apk`.
- App version is `V1.2.1`.
- Home `错题库 / 导出 PDF` is in `录入错题`.
- Library top return area has only the `返回首页` button.
- Library export area does not show `选择当前显示错题`.
- Selecting a mistake in a long library does not jump back to the top.
- Toggling selection or saving subject in detail does not jump back to the top.
- PDF export still supports 1/2/4 questions per page.
- Data backup export/import remains available from the home `导出 / 导入` card.
