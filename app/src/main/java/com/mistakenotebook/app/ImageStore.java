package com.mistakenotebook.app;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

final class ImageStore {
    private final Context context;

    ImageStore(Context context) {
        this.context = context;
    }

    File originalDir() {
        File dir = new File(context.getFilesDir(), "mistakes/original");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    File processedDir() {
        File dir = new File(context.getFilesDir(), "mistakes/processed");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    File exportDir() {
        File dir = new File(context.getFilesDir(), "exports");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    String saveFromUri(Uri uri) throws Exception {
        ContentResolver resolver = context.getContentResolver();
        InputStream input = resolver.openInputStream(uri);
        if (input == null) throw new IllegalStateException("无法读取图片");
        Bitmap bitmap;
        try {
            bitmap = BitmapFactory.decodeStream(input);
        } finally {
            input.close();
        }
        if (bitmap == null) throw new IllegalStateException("图片格式不支持");
        return saveBitmap(limitSize(bitmap, 2200), originalDir());
    }

    String saveBitmap(Bitmap bitmap, File dir) throws Exception {
        File file = new File(dir, UUID.randomUUID() + ".jpg");
        FileOutputStream output = new FileOutputStream(file);
        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output);
        } finally {
            output.close();
        }
        return file.getAbsolutePath();
    }

    Bitmap load(String path) {
        return BitmapFactory.decodeFile(path);
    }

    Bitmap limitSize(Bitmap bitmap, int maxSide) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int longSide = Math.max(width, height);
        if (longSide <= maxSide) return bitmap;
        float scale = maxSide / (float) longSide;
        return Bitmap.createScaledBitmap(bitmap, Math.round(width * scale), Math.round(height * scale), true);
    }
}

