package com.studenthub.app;

import android.content.Context;
import android.content.SharedPreferences;

public final class NotificationSettings {
    private static final String PREF = "studenthub_notifications";

    private NotificationSettings() {}

    public static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static boolean global(Context c) { return prefs(c).getBoolean("global", true); }
    public static boolean announcements(Context c) { return prefs(c).getBoolean("announcements", true); }
    public static boolean appEnabled(Context c) { return prefs(c).getBoolean("app_enabled", true); }
    public static boolean webAppEnabled(Context c) { return prefs(c).getBoolean("web_app", true); }

    // all = all messages, mentions = mentions/replies/tags only, off = none
    public static String messageMode(Context c) {
        return prefs(c).getString("message_mode", "all");
    }

    public static boolean classEnabled(Context c, String classId) {
        if (classId == null || classId.isEmpty()) return true;
        return prefs(c).getBoolean("class_" + classId, true);
    }

    public static void save(Context c, boolean global, boolean announcements,
                            boolean appEnabled, boolean webAppEnabled, String mode) {
        prefs(c).edit()
                .putBoolean("global", global)
                .putBoolean("announcements", announcements)
                .putBoolean("app_enabled", appEnabled)
                .putBoolean("web_app", webAppEnabled)
                .putString("message_mode", mode == null ? "all" : mode)
                .apply();
    }

    // Individual setters used by the JS notification-settings bridge.
    public static void setAppEnabled(Context c, boolean value) {
        prefs(c).edit().putBoolean("app_enabled", value).apply();
    }

    public static void setGlobalEnabled(Context c, boolean value) {
        prefs(c).edit().putBoolean("global", value).apply();
    }

    public static void setAnnouncementEnabled(Context c, boolean value) {
        prefs(c).edit().putBoolean("announcements", value).apply();
    }

    public static void setWebAppEnabled(Context c, boolean value) {
        prefs(c).edit().putBoolean("web_app", value).apply();
    }

    public static void setMessageMode(Context c, String mode) {
        prefs(c).edit().putString("message_mode", mode == null ? "all" : mode).apply();
    }

    public static void setClassEnabled(Context c, String classId, boolean value) {
        if (classId == null || classId.isEmpty()) return;
        prefs(c).edit().putBoolean("class_" + classId, value).apply();
    }

    // is/get-prefixed aliases used by the JS notification-settings bridge.
    public static boolean isAppEnabled(Context c) { return appEnabled(c); }
    public static boolean isGlobalEnabled(Context c) { return global(c); }
    public static boolean isAnnouncementEnabled(Context c) { return announcements(c); }
    public static boolean isWebAppEnabled(Context c) { return webAppEnabled(c); }
    public static String getMessageMode(Context c) { return messageMode(c); }
    public static boolean isClassEnabled(Context c, String classId) { return classEnabled(c, classId); }
}
