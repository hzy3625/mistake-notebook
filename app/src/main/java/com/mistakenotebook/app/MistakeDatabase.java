package com.mistakenotebook.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

final class MistakeDatabase extends SQLiteOpenHelper {
    MistakeDatabase(Context context) {
        super(context, "mistake-notebook.db", null, 2);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE mistakes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "subject TEXT NOT NULL," +
                "original_image_path TEXT NOT NULL," +
                "processed_image_path TEXT," +
                "use_processed_image INTEGER NOT NULL," +
                "analysis_json TEXT," +
                "clean_question_text TEXT," +
                "created_at INTEGER NOT NULL," +
                "printed INTEGER NOT NULL DEFAULT 0" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE mistakes ADD COLUMN clean_question_text TEXT");
        }
    }

    long insert(Mistake mistake) {
        ContentValues values = new ContentValues();
        values.put("subject", mistake.subject.name());
        values.put("original_image_path", mistake.originalImagePath);
        values.put("processed_image_path", mistake.processedImagePath);
        values.put("use_processed_image", mistake.useProcessedImage ? 1 : 0);
        values.put("analysis_json", mistake.analysisJson);
        values.put("clean_question_text", mistake.cleanQuestionText);
        values.put("created_at", mistake.createdAt);
        values.put("printed", mistake.printed ? 1 : 0);
        return getWritableDatabase().insertOrThrow("mistakes", null, values);
    }

    void updateSubject(long id, Subject subject) {
        ContentValues values = new ContentValues();
        values.put("subject", subject.name());
        getWritableDatabase().update("mistakes", values, "id=?", new String[]{String.valueOf(id)});
    }

    List<Mistake> listAll() {
        ArrayList<Mistake> result = new ArrayList<>();
        Cursor cursor = getReadableDatabase().query(
                "mistakes",
                null,
                null,
                null,
                null,
                null,
                "created_at DESC"
        );
        try {
            while (cursor.moveToNext()) result.add(fromCursor(cursor));
        } finally {
            cursor.close();
        }
        return result;
    }

    void markPrinted(List<Long> ids) {
        SQLiteDatabase db = getWritableDatabase();
        for (Long id : ids) {
            ContentValues values = new ContentValues();
            values.put("printed", 1);
            db.update("mistakes", values, "id=?", new String[]{String.valueOf(id)});
        }
    }

    void delete(long id) {
        Mistake target = null;
        for (Mistake mistake : listAll()) {
            if (mistake.id == id) {
                target = mistake;
                break;
            }
        }
        getWritableDatabase().delete("mistakes", "id=?", new String[]{String.valueOf(id)});
        if (target != null) {
            deleteFile(target.originalImagePath);
            deleteFile(target.processedImagePath);
        }
    }

    private Mistake fromCursor(Cursor cursor) {
        Mistake mistake = new Mistake();
        mistake.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        mistake.subject = Subject.fromName(cursor.getString(cursor.getColumnIndexOrThrow("subject")));
        mistake.originalImagePath = cursor.getString(cursor.getColumnIndexOrThrow("original_image_path"));
        mistake.processedImagePath = cursor.getString(cursor.getColumnIndexOrThrow("processed_image_path"));
        mistake.useProcessedImage = cursor.getInt(cursor.getColumnIndexOrThrow("use_processed_image")) == 1;
        mistake.analysisJson = cursor.getString(cursor.getColumnIndexOrThrow("analysis_json"));
        int cleanTextIndex = cursor.getColumnIndex("clean_question_text");
        mistake.cleanQuestionText = cleanTextIndex >= 0 ? cursor.getString(cleanTextIndex) : "";
        mistake.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));
        mistake.printed = cursor.getInt(cursor.getColumnIndexOrThrow("printed")) == 1;
        return mistake;
    }

    private void deleteFile(String path) {
        if (path == null || path.length() == 0) return;
        File file = new File(path);
        if (file.exists()) file.delete();
    }
}
