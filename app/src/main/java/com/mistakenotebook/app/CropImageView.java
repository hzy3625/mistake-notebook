package com.mistakenotebook.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

final class CropImageView extends View {
    private static final int NONE = 0;
    private static final int MOVE = 1;
    private static final int RESIZE_LEFT_TOP = 2;
    private static final int RESIZE_RIGHT_TOP = 3;
    private static final int RESIZE_LEFT_BOTTOM = 4;
    private static final int RESIZE_RIGHT_BOTTOM = 5;

    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint handlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF imageRect = new RectF();
    private final RectF cropRect = new RectF();
    private Bitmap bitmap;
    private boolean cropInitialized;
    private int mode = NONE;
    private float lastX;
    private float lastY;

    CropImageView(Context context) {
        super(context);
        dimPaint.setColor(0x88000000);
        borderPaint.setColor(0xFFFFFFFF);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dp(2));
        handlePaint.setColor(0xFF1F7A5C);
        handlePaint.setStyle(Paint.Style.FILL);
        setBackgroundColor(0xFF111111);
    }

    void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
        this.cropInitialized = false;
        invalidate();
    }

    Bitmap crop() {
        if (bitmap == null) throw new IllegalStateException("图片不存在");
        if (imageRect.width() <= 0 || imageRect.height() <= 0) throw new IllegalStateException("裁截区域无效");
        float scaleX = bitmap.getWidth() / imageRect.width();
        float scaleY = bitmap.getHeight() / imageRect.height();
        int left = clamp(Math.round((cropRect.left - imageRect.left) * scaleX), 0, bitmap.getWidth() - 1);
        int top = clamp(Math.round((cropRect.top - imageRect.top) * scaleY), 0, bitmap.getHeight() - 1);
        int right = clamp(Math.round((cropRect.right - imageRect.left) * scaleX), left + 1, bitmap.getWidth());
        int bottom = clamp(Math.round((cropRect.bottom - imageRect.top) * scaleY), top + 1, bitmap.getHeight());
        return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap == null) return;
        updateImageRect();
        if (!cropInitialized) initCropRect();
        canvas.drawBitmap(bitmap, null, imageRect, bitmapPaint);
        drawDim(canvas);
        canvas.drawRect(cropRect, borderPaint);
        drawHandles(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (bitmap == null || imageRect.isEmpty()) return true;
        float x = event.getX();
        float y = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                mode = hitMode(x, y);
                lastX = x;
                lastY = y;
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = x - lastX;
                float dy = y - lastY;
                updateCrop(dx, dy);
                lastX = x;
                lastY = y;
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                mode = NONE;
                return true;
            default:
                return true;
        }
    }

    private void updateImageRect() {
        float viewWidth = getWidth();
        float viewHeight = getHeight();
        float scale = Math.min(viewWidth / bitmap.getWidth(), viewHeight / bitmap.getHeight());
        float width = bitmap.getWidth() * scale;
        float height = bitmap.getHeight() * scale;
        float left = (viewWidth - width) / 2f;
        float top = (viewHeight - height) / 2f;
        imageRect.set(left, top, left + width, top + height);
    }

    private void initCropRect() {
        float insetX = imageRect.width() * 0.08f;
        float insetY = imageRect.height() * 0.08f;
        cropRect.set(imageRect.left + insetX, imageRect.top + insetY, imageRect.right - insetX, imageRect.bottom - insetY);
        cropInitialized = true;
    }

    private void drawDim(Canvas canvas) {
        canvas.drawRect(imageRect.left, imageRect.top, imageRect.right, cropRect.top, dimPaint);
        canvas.drawRect(imageRect.left, cropRect.bottom, imageRect.right, imageRect.bottom, dimPaint);
        canvas.drawRect(imageRect.left, cropRect.top, cropRect.left, cropRect.bottom, dimPaint);
        canvas.drawRect(cropRect.right, cropRect.top, imageRect.right, cropRect.bottom, dimPaint);
    }

    private void drawHandles(Canvas canvas) {
        float r = dp(7);
        canvas.drawCircle(cropRect.left, cropRect.top, r, handlePaint);
        canvas.drawCircle(cropRect.right, cropRect.top, r, handlePaint);
        canvas.drawCircle(cropRect.left, cropRect.bottom, r, handlePaint);
        canvas.drawCircle(cropRect.right, cropRect.bottom, r, handlePaint);
    }

    private int hitMode(float x, float y) {
        float hit = dp(28);
        if (near(x, y, cropRect.left, cropRect.top, hit)) return RESIZE_LEFT_TOP;
        if (near(x, y, cropRect.right, cropRect.top, hit)) return RESIZE_RIGHT_TOP;
        if (near(x, y, cropRect.left, cropRect.bottom, hit)) return RESIZE_LEFT_BOTTOM;
        if (near(x, y, cropRect.right, cropRect.bottom, hit)) return RESIZE_RIGHT_BOTTOM;
        return cropRect.contains(x, y) ? MOVE : NONE;
    }

    private boolean near(float x, float y, float targetX, float targetY, float hit) {
        return Math.abs(x - targetX) <= hit && Math.abs(y - targetY) <= hit;
    }

    private void updateCrop(float dx, float dy) {
        float minSize = dp(64);
        if (mode == MOVE) {
            cropRect.offset(dx, dy);
            if (cropRect.left < imageRect.left) cropRect.offset(imageRect.left - cropRect.left, 0);
            if (cropRect.right > imageRect.right) cropRect.offset(imageRect.right - cropRect.right, 0);
            if (cropRect.top < imageRect.top) cropRect.offset(0, imageRect.top - cropRect.top);
            if (cropRect.bottom > imageRect.bottom) cropRect.offset(0, imageRect.bottom - cropRect.bottom);
            return;
        }
        if (mode == RESIZE_LEFT_TOP || mode == RESIZE_LEFT_BOTTOM) {
            cropRect.left = clamp(cropRect.left + dx, imageRect.left, cropRect.right - minSize);
        }
        if (mode == RESIZE_RIGHT_TOP || mode == RESIZE_RIGHT_BOTTOM) {
            cropRect.right = clamp(cropRect.right + dx, cropRect.left + minSize, imageRect.right);
        }
        if (mode == RESIZE_LEFT_TOP || mode == RESIZE_RIGHT_TOP) {
            cropRect.top = clamp(cropRect.top + dy, imageRect.top, cropRect.bottom - minSize);
        }
        if (mode == RESIZE_LEFT_BOTTOM || mode == RESIZE_RIGHT_BOTTOM) {
            cropRect.bottom = clamp(cropRect.bottom + dy, cropRect.top + minSize, imageRect.bottom);
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
