package com.mistakenotebook.app;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.MatrixCursor;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileNotFoundException;

public final class SimpleFileProvider extends ContentProvider {
    static final String AUTHORITY = "com.mistakenotebook.app.fileprovider";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public String getType(Uri uri) {
        File file = fileFor(uri);
        String ext = MimeTypeMap.getFileExtensionFromUrl(file.getName());
        String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        return type == null ? "application/octet-stream" : type;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File file = fileFor(uri);
        if ("w".equals(mode) || "wt".equals(mode) || "rw".equals(mode)) {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_TRUNCATE | ParcelFileDescriptor.MODE_WRITE_ONLY);
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    static Uri uriFor(File file, File root) {
        String relative = root.toURI().relativize(file.toURI()).getPath();
        return new Uri.Builder()
                .scheme("content")
                .authority(AUTHORITY)
                .appendPath("file")
                .appendEncodedPath(Uri.encode(relative, "/"))
                .build();
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        File file = fileFor(uri);
        String[] columns = projection == null ? new String[]{
                OpenableColumns.DISPLAY_NAME,
                OpenableColumns.SIZE
        } : projection;
        MatrixCursor cursor = new MatrixCursor(columns, 1);
        MatrixCursor.RowBuilder row = cursor.newRow();
        for (String column : columns) {
            if (OpenableColumns.DISPLAY_NAME.equals(column)) {
                row.add(file.getName());
            } else if (OpenableColumns.SIZE.equals(column)) {
                row.add(file.length());
            } else {
                row.add(null);
            }
        }
        return cursor;
    }

    private File fileFor(Uri uri) {
        File root = getContext().getFilesDir();
        String encoded = uri.getEncodedPath();
        if (encoded == null || !encoded.startsWith("/file/")) {
            throw new IllegalArgumentException("路径无效");
        }
        String relative = Uri.decode(encoded.substring("/file/".length()));
        File file = new File(root, relative);
        try {
            String rootPath = root.getCanonicalPath();
            String filePath = file.getCanonicalPath();
            if (!filePath.startsWith(rootPath)) throw new IllegalArgumentException("路径越界");
        } catch (Exception e) {
            throw new IllegalArgumentException("路径无效");
        }
        return file;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
