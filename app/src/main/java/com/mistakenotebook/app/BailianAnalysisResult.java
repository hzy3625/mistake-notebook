package com.mistakenotebook.app;

import java.util.ArrayList;
import java.util.List;

final class BailianAnalysisResult {
    final Subject subject;
    final double subjectConfidence;
    final boolean hasHandwriting;
    final String handwritingRisk;
    final boolean canAutoClean;
    final String suggestion;
    final String rawJson;
    final boolean parsed;
    final List<int[]> handwritingRegions;
    final String cleanQuestionText;

    BailianAnalysisResult(
            Subject subject,
            double subjectConfidence,
            boolean hasHandwriting,
            String handwritingRisk,
            boolean canAutoClean,
            String suggestion,
            String rawJson,
            boolean parsed,
            List<int[]> handwritingRegions,
            String cleanQuestionText
    ) {
        this.subject = subject;
        this.subjectConfidence = subjectConfidence;
        this.hasHandwriting = hasHandwriting;
        this.handwritingRisk = handwritingRisk == null ? "UNKNOWN" : handwritingRisk;
        this.canAutoClean = canAutoClean;
        this.suggestion = suggestion == null ? "" : suggestion;
        this.rawJson = rawJson == null ? "" : rawJson;
        this.parsed = parsed;
        this.handwritingRegions = handwritingRegions == null ? new ArrayList<>() : handwritingRegions;
        this.cleanQuestionText = cleanQuestionText == null ? "" : cleanQuestionText.trim();
    }

    static BailianAnalysisResult manual() {
        return new BailianAnalysisResult(Subject.OTHER, 0, false, "UNKNOWN", false, "未完成题目提取，请手动确认。", "{}", false, new ArrayList<>(), "");
    }

    static BailianAnalysisResult parseError(String rawText) {
        return new BailianAnalysisResult(
                Subject.OTHER,
                0,
                true,
                "UNKNOWN",
                false,
                "模型响应未能解析为预期 JSON。请重新提取题目，或检查模型是否返回了干净题目文本。",
                rawText == null ? "" : rawText,
                false,
                new ArrayList<>(),
                ""
        );
    }

    static BailianAnalysisResult cleanTextOnly(String text) {
        return new BailianAnalysisResult(
                Subject.OTHER,
                0,
                true,
                "UNKNOWN",
                false,
                "模型返回了纯文本题目，已作为干净题目文本保存。",
                text == null ? "" : text,
                true,
                new ArrayList<>(),
                text == null ? "" : text
        );
    }
}
