package com.mistakenotebook.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class SecurePrefs {
    static final String DEFAULT_MODEL_ID = "qwen3-vl-plus";
    static final String DEFAULT_MODEL_NAME = "Qwen3-VL-Plus";
    private static final String PREFS = "secure_settings";
    private static final String KEY_ALIAS = "mistake_notebook_api_key";
    private static final String API_KEY = "api_key";
    private static final String API_IV = "api_iv";
    private static final String MODEL = "model";

    private final SharedPreferences prefs;

    SecurePrefs(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    void saveApiKey(String apiKey) throws Exception {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            prefs.edit().remove(API_KEY).remove(API_IV).apply();
            return;
        }
        SecretKey key = getOrCreateKey();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encrypted = cipher.doFinal(apiKey.trim().getBytes(StandardCharsets.UTF_8));
        byte[] iv = cipher.getIV();
        prefs.edit()
                .putString(API_KEY, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .putString(API_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                .apply();
    }

    String loadApiKey() throws Exception {
        String encrypted = prefs.getString(API_KEY, null);
        String ivText = prefs.getString(API_IV, null);
        if (encrypted == null || ivText == null) return "";
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                new GCMParameterSpec(128, Base64.decode(ivText, Base64.NO_WRAP))
        );
        byte[] plain = cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP));
        return new String(plain, StandardCharsets.UTF_8);
    }

    String apiKeyPreview() {
        try {
            String value = loadApiKey();
            if (value.length() <= 8) return value.isEmpty() ? "未配置" : "已配置";
            return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
        } catch (Exception e) {
            return "读取失败";
        }
    }

    void saveModel(String model) {
        prefs.edit().putString(MODEL, normalizeModel(model)).apply();
    }

    String loadModel() {
        String model = normalizeModel(prefs.getString(MODEL, DEFAULT_MODEL_ID));
        if (!model.equals(prefs.getString(MODEL, DEFAULT_MODEL_ID))) {
            prefs.edit().putString(MODEL, model).apply();
        }
        return model;
    }

    static String normalizeModel(String model) {
        if (model == null) return DEFAULT_MODEL_ID;
        String value = model.trim();
        if (value.isEmpty()) return DEFAULT_MODEL_ID;
        if ("qwen-vl-plus".equalsIgnoreCase(value)) return DEFAULT_MODEL_ID;
        if (DEFAULT_MODEL_NAME.equalsIgnoreCase(value)) return DEFAULT_MODEL_ID;
        return value;
    }

    private SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return (SecretKey) keyStore.getKey(KEY_ALIAS, null);
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
        )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build();
        generator.init(spec);
        return generator.generateKey();
    }
}
