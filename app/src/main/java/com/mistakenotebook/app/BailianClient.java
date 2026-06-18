package com.mistakenotebook.app;

import android.graphics.Bitmap;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class BailianClient {
    private static final String ENDPOINT = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

    BailianAnalysisResult analyze(String apiKey, String model, Bitmap bitmap) throws Exception {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("未读取到已保存的百炼 API Key，请进入系统设置输入并保存后再提取题目");
        }
        String imageData = toBase64Jpeg(bitmap);
        String body = requestBody(model, imageData);

        HttpURLConnection connection = (HttpURLConnection) new URL(ENDPOINT).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        OutputStream output = connection.getOutputStream();
        try {
            output.write(body.getBytes(StandardCharsets.UTF_8));
        } finally {
            output.close();
        }

        int code = connection.getResponseCode();
        InputStream input = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
        String text = readAll(input);
        if (code < 200 || code >= 300) throw new IllegalStateException("百炼请求失败: HTTP " + code);
        return parseResult(text);
    }

    String testConnection(String apiKey, String model) throws Exception {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("请先输入百炼 API Key");
        }
        String body = "{"
                + "\"model\":\"" + escape(SecurePrefs.normalizeModel(model)) + "\","
                + "\"messages\":[{\"role\":\"user\",\"content\":\"只回复 OK\"}],"
                + "\"temperature\":0"
                + "}";
        HttpURLConnection connection = (HttpURLConnection) new URL(ENDPOINT).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        OutputStream output = connection.getOutputStream();
        try {
            output.write(body.getBytes(StandardCharsets.UTF_8));
        } finally {
            output.close();
        }
        int code = connection.getResponseCode();
        InputStream input = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
        String text = readAll(input);
        if (code < 200 || code >= 300) {
            String message = findFirst(text, "\\\"message\\\"\\s*:\\s*\\\"(.*?)\\\"");
            if (message.length() == 0) message = "HTTP " + code;
            throw new IllegalStateException("百炼连接失败: " + unescapeJson(message));
        }
        return "百炼连接成功";
    }

    private String requestBody(String model, String imageData) {
        String prompt = "你是一个错题图片转写助手。核心任务：从图片中提取适合打印的干净题目文本。"
                + "请优先输出 JSON，不要输出 Markdown。字段为 subject, subject_confidence, suggestion, warnings, clean_question_text。"
                + "subject 必须是 CHINESE, MATH, ENGLISH, PHYSICS, CHEMISTRY, BIOLOGY, HISTORY, GEOGRAPHY, POLITICS, OTHER 之一。"
                + "clean_question_text 是从图片中提取的干净题目文本：只保留印刷题干、题号、选项、表格文字、公式文字；必须去掉学生手写答案、红笔批改、圈画、勾叉、涂改和无关批注。"
                + "不要输出 LaTeX 源码。分数统一输出成 2/7、7/10 这种普通文本格式。保留题号和换行结构，便于打印。"
                + "如果 JSON 输出受限，可以只输出干净题目文本，不要解释。";
        return "{"
                + "\"model\":\"" + escape(SecurePrefs.normalizeModel(model)) + "\","
                + "\"messages\":[{\"role\":\"user\",\"content\":["
                + "{\"type\":\"text\",\"text\":\"" + escape(prompt) + "\"},"
                + "{\"type\":\"image_url\",\"image_url\":{\"url\":\"data:image/jpeg;base64," + imageData + "\"}}"
                + "]}],"
                + "\"temperature\":0"
                + "}";
    }

    private BailianAnalysisResult parseResult(String response) {
        String content = readJsonStringField(response, "content");
        String json = extractJson(content.length() > 0 ? content : response);
        if (!hasRequiredFields(json)) {
            String fallbackText = stripMarkdownFence(content.length() > 0 ? content : response);
            if (fallbackText.length() > 0) {
                return BailianAnalysisResult.cleanTextOnly(CleanTextFormatter.normalize(fallbackText));
            }
            return BailianAnalysisResult.parseError(content.length() > 0 ? content : response);
        }
        Subject subject = Subject.fromName(findJsonString(json, "subject"));
        double confidence = findJsonDouble(json, "subject_confidence", 0);
        boolean hasHandwriting = findJsonBoolean(json, "has_handwriting", false);
        String risk = findJsonString(json, "handwriting_risk");
        boolean canAutoClean = findJsonBoolean(json, "can_auto_clean", false);
        String suggestion = findJsonString(json, "suggestion");
        String cleanQuestionText = CleanTextFormatter.normalize(readJsonStringField(json, "clean_question_text"));
        return new BailianAnalysisResult(subject, confidence, hasHandwriting, risk, canAutoClean, suggestion, json, true, new ArrayList<>(), cleanQuestionText);
    }

    private String toBase64Jpeg(Bitmap bitmap) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 86, output);
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP);
    }

    private String readAll(InputStream input) throws Exception {
        if (input == null) return "";
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
        return output.toString("UTF-8");
    }

    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) return text.substring(start, end + 1);
        return "{}";
    }

    private boolean hasRequiredFields(String json) {
        return json.contains("\"clean_question_text\"");
    }

    private String stripMarkdownFence(String text) {
        String value = text == null ? "" : text.trim();
        if (value.startsWith("```")) {
            int firstLine = value.indexOf('\n');
            int lastFence = value.lastIndexOf("```");
            if (firstLine >= 0 && lastFence > firstLine) {
                value = value.substring(firstLine + 1, lastFence).trim();
            }
        }
        return value;
    }

    private String findJsonString(String json, String key) {
        return unescapeJson(findFirst(json, "\\\"" + key + "\\\"\\s*:\\s*\\\"(.*?)\\\""));
    }

    private double findJsonDouble(String json, String key, double fallback) {
        String value = findFirst(json, "\\\"" + key + "\\\"\\s*:\\s*([0-9.]+)");
        if (value.length() == 0) return fallback;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private boolean findJsonBoolean(String json, String key, boolean fallback) {
        String value = findFirst(json, "\\\"" + key + "\\\"\\s*:\\s*(true|false)");
        if (value.length() == 0) return fallback;
        return Boolean.parseBoolean(value);
    }

    private String findFirst(String text, String regex) {
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text == null ? "" : text);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String readJsonStringField(String text, String key) {
        String marker = "\"" + key + "\"";
        int keyIndex = text.indexOf(marker);
        if (keyIndex < 0) return "";
        int colon = text.indexOf(':', keyIndex + marker.length());
        if (colon < 0) return "";
        int start = text.indexOf('"', colon + 1);
        if (start < 0) return "";
        StringBuilder builder = new StringBuilder();
        boolean escaping = false;
        for (int i = start + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escaping) {
                if (c == 'n') builder.append('\n');
                else if (c == 't') builder.append('\t');
                else builder.append(c);
                escaping = false;
            } else if (c == '\\') {
                escaping = true;
            } else if (c == '"') {
                return builder.toString();
            } else {
                builder.append(c);
            }
        }
        return "";
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private String unescapeJson(String value) {
        return value
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
