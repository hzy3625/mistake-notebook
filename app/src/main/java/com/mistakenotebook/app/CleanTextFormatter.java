package com.mistakenotebook.app;

final class CleanTextFormatter {
    private CleanTextFormatter() {
    }

    static String normalize(String text) {
        if (text == null) return "";
        String value = text
                .replace("\\(", "")
                .replace("\\)", "")
                .replace("\\[", "")
                .replace("\\]", "")
                .replace("\\quad", "    ")
                .replace("\\,", " ")
                .replace("\\;", " ")
                .replace("\\ ", " ")
                .replace("\\left", "")
                .replace("\\right", "")
                .replace("\\times", "×")
                .replace("\\div", "÷")
                .replace("\\pm", "±")
                .replace("\\ge", "≥")
                .replace("\\le", "≤")
                .replace("\\neq", "≠")
                .replace("\\approx", "≈")
                .replace("\\ldots", "...")
                .replace("\\cdots", "...")
                .replace("$", "");

        value = replaceFractions(value);
        value = value
                .replace("\\=", "=")
                .replace("\\+", "+")
                .replace("\\-", "-")
                .replace("\\_", "_")
                .replace("{", "")
                .replace("}", "")
                .replace("\\", "");
        value = value.replaceAll("\\s+([，。；：、,.!?])", "$1");
        value = value.replaceAll("[ \\t]+\\n", "\n");
        value = value.replaceAll("[ \\t]{2,}", " ");
        value = value.replaceAll("\\n{3,}", "\n\n");
        return value.trim();
    }

    private static String replaceFractions(String value) {
        String marker = "\\frac{";
        StringBuilder out = new StringBuilder();
        int index = 0;
        while (index < value.length()) {
            int start = value.indexOf(marker, index);
            if (start < 0) {
                out.append(value.substring(index));
                break;
            }
            out.append(value, index, start);
            int numeratorStart = start + marker.length();
            int numeratorEnd = value.indexOf('}', numeratorStart);
            if (numeratorEnd < 0 || numeratorEnd + 1 >= value.length() || value.charAt(numeratorEnd + 1) != '{') {
                out.append(value.charAt(start));
                index = start + 1;
                continue;
            }
            int denominatorStart = numeratorEnd + 2;
            int denominatorEnd = value.indexOf('}', denominatorStart);
            if (denominatorEnd < 0) {
                out.append(value.charAt(start));
                index = start + 1;
                continue;
            }
            out.append(value, numeratorStart, numeratorEnd)
                    .append('/')
                    .append(value, denominatorStart, denominatorEnd);
            index = denominatorEnd + 1;
        }
        return out.toString();
    }
}
