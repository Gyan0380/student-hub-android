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
}
