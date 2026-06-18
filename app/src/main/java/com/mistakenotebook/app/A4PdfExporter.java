package com.mistakenotebook.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final class A4PdfExporter {
    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final int MARGIN = 36;
    private static final int IMAGE_MARGIN = 12;

    private final Context context;
    private final ImageStore imageStore;

    A4PdfExporter(Context context, ImageStore imageStore) {
        this.context = context;
        this.imageStore = imageStore;
    }

    File export(List<Mistake> mistakes, int perPage) throws Exception {
        if (mistakes.isEmpty()) throw new IllegalStateException("请先选择错题");
        if (perPage != 2 && perPage != 4) perPage = 2;

        PdfDocument document = new PdfDocument();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(0xFFDDDDDD);
        linePaint.setStrokeWidth(1f);
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFF111111);
        textPaint.setTextSize(15f);
        Paint fractionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fractionPaint.setColor(0xFF111111);
        fractionPaint.setTextSize(13f);
        Paint metaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        metaPaint.setColor(0xFF666666);
        metaPaint.setTextSize(10f);
        metaPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        int pageCount = (int) Math.ceil(mistakes.size() / (double) perPage);
        int index = 0;
        for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            canvas.drawColor(0xFFFFFFFF);
            float contentHeight = PAGE_HEIGHT - MARGIN * 2f;
            float slotHeight = contentHeight / perPage;
            for (int i = 0; i < perPage && index < mistakes.size(); i++, index++) {
                Mistake mistake = mistakes.get(index);
                float top = MARGIN + i * slotHeight;
                drawMistake(canvas, paint, linePaint, textPaint, fractionPaint, metaPaint, mistake, top, slotHeight, perPage);
            }
            document.finishPage(page);
        }

        String name = "mistakes-" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date()) + ".pdf";
        File file = new File(imageStore.exportDir(), name);
        FileOutputStream output = new FileOutputStream(file);
        try {
            document.writeTo(output);
        } finally {
            output.close();
            document.close();
        }
        return file;
    }

    private void drawMistake(Canvas canvas, Paint paint, Paint linePaint, Paint textPaint, Paint fractionPaint, Paint metaPaint, Mistake mistake, float top, float slotHeight, int perPage) {
        if (mistake.cleanQuestionText != null && mistake.cleanQuestionText.trim().length() > 0) {
            drawTextMistake(canvas, linePaint, textPaint, fractionPaint, metaPaint, mistake, top, slotHeight);
            return;
        }
        Bitmap bitmap = BitmapFactory.decodeFile(mistake.exportImagePath());
        if (bitmap == null) return;
        float left = IMAGE_MARGIN;
        float width = PAGE_WIDTH - IMAGE_MARGIN * 2f;
        float maxImageHeight = slotHeight * (perPage == 2 ? 0.97f : 0.92f);
        float scale = Math.min(width / bitmap.getWidth(), maxImageHeight / bitmap.getHeight());
        float drawWidth = bitmap.getWidth() * scale;
        float drawHeight = bitmap.getHeight() * scale;
        float drawLeft = left + (width - drawWidth) / 2f;
        RectF dst = new RectF(drawLeft, top + 2f, drawLeft + drawWidth, top + 2f + drawHeight);
        canvas.drawBitmap(bitmap, null, dst, paint);
    }

    private void drawTextMistake(Canvas canvas, Paint linePaint, Paint textPaint, Paint fractionPaint, Paint metaPaint, Mistake mistake, float top, float slotHeight) {
        float left = MARGIN;
        float width = PAGE_WIDTH - MARGIN * 2f;
        float y = top + 12;
        canvas.drawText("#" + mistake.id + " " + mistake.subject.label + " 题目文本", left, y, metaPaint);
        y += 18;

        float lineHeight = 28f;
        float textBottom = top + slotHeight * 0.68f;
        for (String line : wrapText(mistake.cleanQuestionText.trim(), textPaint, width)) {
            if (y + lineHeight > textBottom) {
                canvas.drawText("...", left, y, textPaint);
                y += lineHeight;
                break;
            }
            drawRichLine(canvas, line, left, y, textPaint, fractionPaint, linePaint);
            y += lineHeight;
        }

    }

    private List<String> wrapText(String text, Paint paint, float maxWidth) {
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        String[] paragraphs = text.split("\\n");
        for (String paragraph : paragraphs) {
            String current = "";
            for (int i = 0; i < paragraph.length(); i++) {
                String next = current + paragraph.charAt(i);
                if (paint.measureText(next) > maxWidth && current.length() > 0) {
                    lines.add(current);
                    current = String.valueOf(paragraph.charAt(i));
                } else {
                    current = next;
                }
            }
            if (current.length() > 0) lines.add(current);
            else lines.add("");
        }
        return lines;
    }

    private void drawRichLine(Canvas canvas, String line, float x, float baseline, Paint textPaint, Paint fractionPaint, Paint linePaint) {
        float cursor = x;
        int i = 0;
        while (i < line.length()) {
            char c = line.charAt(i);
            if (c == '$') {
                i++;
                continue;
            }
            Fraction fraction = readFraction(line, i);
            if (fraction != null) {
                float numeratorWidth = fractionPaint.measureText(fraction.numerator);
                float denominatorWidth = fractionPaint.measureText(fraction.denominator);
                float width = Math.max(numeratorWidth, denominatorWidth) + 8f;
                float center = cursor + width / 2f;
                canvas.drawText(fraction.numerator, center - numeratorWidth / 2f, baseline - 9f, fractionPaint);
                canvas.drawLine(cursor + 2f, baseline - 5f, cursor + width - 2f, baseline - 5f, linePaint);
                canvas.drawText(fraction.denominator, center - denominatorWidth / 2f, baseline + 9f, fractionPaint);
                cursor += width;
                i = fraction.endIndex;
            } else {
                String text = String.valueOf(c);
                canvas.drawText(text, cursor, baseline, textPaint);
                cursor += textPaint.measureText(text);
                i++;
            }
        }
    }

    private Fraction readFraction(String line, int start) {
        int i = start;
        while (i < line.length() && Character.isDigit(line.charAt(i))) i++;
        if (i == start || i >= line.length() || line.charAt(i) != '/') return null;
        int denominatorStart = i + 1;
        int j = denominatorStart;
        while (j < line.length() && Character.isDigit(line.charAt(j))) j++;
        if (j == denominatorStart) return null;
        if (start > 0 && Character.isLetterOrDigit(line.charAt(start - 1))) return null;
        if (j < line.length() && Character.isLetterOrDigit(line.charAt(j))) return null;
        return new Fraction(line.substring(start, i), line.substring(denominatorStart, j), j);
    }

    private static final class Fraction {
        final String numerator;
        final String denominator;
        final int endIndex;

        Fraction(String numerator, String denominator, int endIndex) {
            this.numerator = numerator;
            this.denominator = denominator;
            this.endIndex = endIndex;
        }
    }
}
