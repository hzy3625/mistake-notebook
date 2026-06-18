package com.mistakenotebook.app;

final class Mistake {
    long id;
    Subject subject;
    String originalImagePath;
    String processedImagePath;
    boolean useProcessedImage;
    String analysisJson;
    String cleanQuestionText;
    long createdAt;
    boolean printed;

    String exportImagePath() {
        if (useProcessedImage && processedImagePath != null && processedImagePath.length() > 0) {
            return processedImagePath;
        }
        return originalImagePath;
    }
}
