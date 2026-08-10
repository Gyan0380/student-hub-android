package com.studenthub.app;

import android.content.Context;
import android.content.SharedPreferences;

public final class NotificationSettings {

    private static final String PREF =
            "student_hub_notification_settings";

    private NotificationSettings() {
    }

    private static SharedPreferences prefs(Context context) {

        return context.getSharedPreferences(
                PREF,
                Context.MODE_PRIVATE
        );
    }

    public static boolean isAppEnabled(Context context) {

        return prefs(context)
                .getBoolean("app_enabled", true);
    }

    public static boolean isGlobalEnabled(Context context) {

        return prefs(context)
                .getBoolean("global_enabled", true);
    }

    public static boolean isAnnouncementEnabled(Context context) {

        return prefs(context)
                .getBoolean("announcement_enabled", true);
    }

    public static boolean isWebAppEnabled(Context context) {

        return prefs(context)
                .getBoolean("web_app_enabled", true);
    }

    public static String getMessageMode(Context context) {

        return prefs(context)
                .getString("message_mode", "all");
    }

    public static boolean isClassEnabled(
            Context context,
            String className
    ) {

        if (className == null) {
            return true;
        }

        return prefs(context)
                .getBoolean(
                        "class_" + className,
                        true
                );
    }

    public static void setAppEnabled(
            Context context,
            boolean value
    ) {

        prefs(context)
                .edit()
                .putBoolean("app_enabled", value)
                .apply();
    }

    public static void setGlobalEnabled(
            Context context,
            boolean value
    ) {

        prefs(context)
                .edit()
                .putBoolean("global_enabled", value)
                .apply();
    }

    public static void setAnnouncementEnabled(
            Context context,
            boolean value
    ) {

        prefs(context)
                .edit()
                .putBoolean(
                        "announcement_enabled",
                        value
                )
                .apply();
    }

    public static void setWebAppEnabled(
            Context context,
            boolean value
    ) {

        prefs(context)
                .edit()
                .putBoolean(
                        "web_app_enabled",
                        value
                )
                .apply();
    }

    public static void setMessageMode(
            Context context,
            String mode
    ) {

        prefs(context)
                .edit()
                .putString(
                        "message_mode",
                        mode
                )
                .apply();
    }

    public static void setClassEnabled(
            Context context,
            String className,
            boolean value
    ) {

        if (className == null) {
            return;
        }

        prefs(context)
                .edit()
                .putBoolean(
                        "class_" + className,
                        value
                )
                .apply();
    }

    public static boolean shouldShow(
            Context context,
            String type,
            String className,
            boolean mentioned
    ) {

        if (!isAppEnabled(context)) {
            return false;
        }

        if ("force".equals(type)) {
            return true;
        }

        if ("global".equals(type)) {
            return isGlobalEnabled(context);
        }

        if ("announcement".equals(type)) {
            return isAnnouncementEnabled(context);
        }

        if ("class".equals(type)) {

            return isClassEnabled(
                    context,
                    className
            );
        }

        if ("web_app".equals(type)) {

            return isWebAppEnabled(context);
        }

        String mode =
                getMessageMode(context);

        if ("off".equals(mode)) {
            return false;
        }

        if ("mentions".equals(mode)) {
            return mentioned;
        }

        return true;
    }
}
