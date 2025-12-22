package com.example.collab.util;

public final class HtmlUtil {
    private HtmlUtil() {
    }

    public static String stripHtml(String html) {
        if (html == null) {
            return "";
        }
        return html.replaceAll("<[^>]*>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
