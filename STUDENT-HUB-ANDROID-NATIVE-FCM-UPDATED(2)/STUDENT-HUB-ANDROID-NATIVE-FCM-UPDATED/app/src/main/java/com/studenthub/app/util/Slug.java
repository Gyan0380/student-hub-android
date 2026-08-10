package com.studenthub.app.util;

/** Same slugify logic as the web app's slugify() in app.js (e.g. "Class 9" -> "class-9"). */
public class Slug {
    public static String slugify(String input) {
        if (input == null) return "";
        return input.trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }
}
