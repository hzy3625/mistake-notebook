# AGENTS.md

## Project Overview

This repository contains the MVP planning and technical documentation for an Android mistake notebook app.

The app helps students:

- Capture mistakes by camera or gallery import.
- Extract clean question text with Alibaba Bailian multimodal models.
- Classify mistakes by subject.
- Save original images and extracted text locally.
- Export selected mistakes to A4 PDF for printing.

## Repository Structure

```text
.
├── AGENTS.md
└── docs
    ├── AGENTS.md
    ├── product
    │   └── mvp-plan.md
    └── engineering
        └── technical-design-and-test-cases.md
```

## General Instructions

- Use English file and directory names.
- Markdown content may be written in Chinese when the target reader is Chinese.
- Keep documents implementation-oriented and concise.
- Preserve original images in all product and technical designs.
- Treat handwriting removal as a high-risk operation; designs must prefer conservative behavior over aggressive automation.
- API keys must not be logged, committed, or stored in plaintext.

## Product Constraints

- The MVP is a local-first Android app.
- Alibaba Bailian API Key is configured by the user in app settings.
- The MVP does not include login, cloud sync, backend services, or account management.
- Multimodal models are used for clean question text extraction, not pixel-level image editing.
- If extraction fails, the app can still save and export the original image.

## Engineering Constraints

- Current Android stack: Java, native Android views, SQLiteOpenHelper, Android platform networking.
- Use Android Keystore or encrypted storage for API Key persistence.
- Use app-private storage for original images.
- Use Android `PdfDocument` for the MVP PDF export path unless a stronger requirement emerges.
- Tests should cover API response parsing, text normalization, data persistence, and PDF pagination.

## Verification

When changing technical behavior or implementation docs, update corresponding test cases in:

```text
docs/engineering/technical-design-and-test-cases.md
```

When changing product scope, update:

```text
docs/product/mvp-plan.md
```
