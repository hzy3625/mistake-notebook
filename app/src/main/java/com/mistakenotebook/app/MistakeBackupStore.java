package com.mistakenotebook.app;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

final class MistakeBackupStore {
    private static final String MANIFEST = "mistakes.tsv";

    private final Context context;
    private final ImageStore imageStore;
    private final MistakeDatabase database;

    MistakeBackupStore(Context context, ImageStore imageStore, MistakeDatabase database) {
        this.context = context;
        this.imageStore = imageStore;
        this.database = database;
    }

    File exportBackup() throws Exception {
        List<Mistake> mistakes = database.listAll();
        if (mistakes.isEmpty()) throw new IllegalStateException("暂无错题可导出");
        String name = "mistake-notebook-backup-" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date()) + ".zip";
        File backup = new File(imageStore.exportDir(), name);
        ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(backup));
        try {
            StringBuilder manifest = new StringBuilder();
            manifest.append("v\t1\n");
            int index = 0;
            for (Mistake mistake : mistakes) {
                String originalEntry = addFile(zip, mistake.originalImagePath, "images/original/" + index + ".jpg");
                String processedEntry = addFile(zip, mistake.processedImagePath, "images/processed/" + index + ".jpg");
                manifest.append("m")
                        .append('\t').append(mistake.subject.name())
                        .append('\t').append(originalEntry)
                        .append('\t').append(processedEntry)
                        .append('\t').append(mistake.useProcessedImage ? "1" : "0")
                        .append('\t').append(mistake.createdAt)
                        .append('\t').append(mistake.printed ? "1" : "0")
                        .append('\t').append(encode(mistake.analysisJson))
                        .append('\t').append(encode(mistake.cleanQuestionText))
                        .append('\n');
                index++;
            }
            zip.putNextEntry(new ZipEntry(MANIFEST));
            zip.write(manifest.toString().getBytes("UTF-8"));
            zip.closeEntry();
        } finally {
            zip.close();
        }
        return backup;
    }

    int importBackup(Uri uri) throws Exception {
        InputStream input = context.getContentResolver().openInputStream(uri);
        if (input == null) throw new IllegalStateException("无法读取备份文件");
        Map<String, byte[]> entries = new HashMap<>();
        String manifest;
        ZipInputStream zip = new ZipInputStream(input);
        try {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                byte[] data = readAll(zip);
                if (MANIFEST.equals(entry.getName())) {
                    manifest = new String(data, "UTF-8");
                    entries.put(MANIFEST, data);
                } else if (entry.getName().startsWith("images/")) {
                    entries.put(entry.getName(), data);
                }
                zip.closeEntry();
            }
        } finally {
            zip.close();
        }
        byte[] manifestBytes = entries.get(MANIFEST);
        if (manifestBytes == null) throw new IllegalStateException("备份文件缺少清单");
        manifest = new String(manifestBytes, "UTF-8");
        int count = 0;
        for (String line : manifest.split("\\n")) {
            if (!line.startsWith("m\t")) continue;
            String[] parts = line.split("\\t", -1);
            if (parts.length < 9) continue;
            Mistake mistake = new Mistake();
            mistake.subject = Subject.fromName(parts[1]);
            mistake.originalImagePath = restoreImage(entries, parts[2], imageStore.originalDir());
            mistake.processedImagePath = restoreImage(entries, parts[3], imageStore.processedDir());
            mistake.useProcessedImage = "1".equals(parts[4]) && mistake.processedImagePath != null;
            mistake.createdAt = parseLong(parts[5], System.currentTimeMillis());
            mistake.printed = "1".equals(parts[6]);
            mistake.analysisJson = decode(parts[7]);
            mistake.cleanQuestionText = decode(parts[8]);
            if (mistake.originalImagePath != null) {
                database.insert(mistake);
                count++;
            }
        }
        return count;
    }

    private String addFile(ZipOutputStream zip, String path, String entryName) throws Exception {
        if (path == null || path.length() == 0) return "";
        File file = new File(path);
        if (!file.exists()) return "";
        zip.putNextEntry(new ZipEntry(entryName));
        FileInputStream input = new FileInputStream(file);
        try {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) zip.write(buffer, 0, read);
        } finally {
            input.close();
            zip.closeEntry();
        }
        return entryName;
    }

    private String restoreImage(Map<String, byte[]> entries, String entryName, File dir) throws Exception {
        if (entryName == null || entryName.length() == 0) return null;
        byte[] data = entries.get(entryName);
        if (data == null) return null;
        return imageStore.saveBytes(data, dir, ".jpg");
    }

    private byte[] readAll(InputStream input) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
        return output.toByteArray();
    }

    private String encode(String value) {
        byte[] bytes = (value == null ? "" : value).getBytes();
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    private String decode(String value) {
        if (value == null || value.length() == 0) return "";
        try {
            return new String(Base64.decode(value, Base64.NO_WRAP));
        } catch (Exception e) {
            return "";
        }
    }

    private long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return fallback;
        }
    }
}
