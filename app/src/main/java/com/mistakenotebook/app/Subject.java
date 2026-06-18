package com.mistakenotebook.app;

enum Subject {
    CHINESE("语文"),
    MATH("数学"),
    ENGLISH("英语"),
    PHYSICS("物理"),
    CHEMISTRY("化学"),
    BIOLOGY("生物"),
    HISTORY("历史"),
    GEOGRAPHY("地理"),
    POLITICS("政治"),
    OTHER("其他");

    final String label;

    Subject(String label) {
        this.label = label;
    }

    static Subject fromName(String value) {
        if (value == null) return OTHER;
        for (Subject subject : values()) {
            if (subject.name().equalsIgnoreCase(value) || subject.label.equals(value)) return subject;
        }
        return OTHER;
    }

    static String[] labels() {
        Subject[] values = values();
        String[] labels = new String[values.length];
        for (int i = 0; i < values.length; i++) labels[i] = values[i].label;
        return labels;
    }
}

