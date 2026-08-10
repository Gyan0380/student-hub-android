package com.studenthub.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class StudentHubMessagingService extends FirebaseMessagingService {
    private static final String CHANNEL_DEFAULT = "studenthub_default";
    private static final String CHANNEL_FORCE = "studenthub_force";

    @Override
    public void onMessageReceived(RemoteMessage message) {
        if (message == null) return;

        String type = get(message, "type", "message");
        boolean force = "force".equalsIgnoreCase(type) ||
                        "important".equalsIgnoreCase(type);

        // Force/important notifications are intentionally independent
        // from ordinary message notification toggles.
        if (!force) {
            if (!NotificationSettings.appEnabled(this)) return;
            if ("announcement".equalsIgnoreCase(type) &&
                    !NotificationSettings.announcements(this)) return;
            if ("global".equalsIgnoreCase(type) &&
                    !NotificationSettings.global(this)) return;

            if (isMessageType(type)) {
                String mode = NotificationSettings.messageMode(this);
                if ("off".equalsIgnoreCase(mode)) return;
                if ("mentions".equalsIgnoreCase(mode) && !isMention(message)) return;
            }

            String classId = message.getData().get("classId");
            if (classId != null && !NotificationSettings.classEnabled(this, classId)) return;
        }

        String title = message.getNotification() != null &&
                message.getNotification().getTitle() != null
                ? message.getNotification().getTitle()
                : get(message, "title", "Student Hub");

        String body = message.getNotification() != null &&
                message.getNotification().getBody() != null
                ? message.getNotification().getBody()
                : get(message, "body", "New notification");

        show(title, body, force);
    }

    private boolean isMessageType(String type) {
        return "message".equalsIgnoreCase(type) ||
               "mention".equalsIgnoreCase(type) ||
               "reply".equalsIgnoreCase(type) ||
               "tag".equalsIgnoreCase(type);
    }

    private boolean isMention(RemoteMessage m) {
        String value = m.getData().get("mentioned");
        return "true".equalsIgnoreCase(value) ||
               "1".equals(value) ||
               "mention".equalsIgnoreCase(m.getData().get("type")) ||
               "reply".equalsIgnoreCase(m.getData().get("type")) ||
               "tag".equalsIgnoreCase(m.getData().get("type"));
    }

    private String get(RemoteMessage m, String key, String fallback) {
        String v = m.getData().get(key);
        return v == null || v.isEmpty() ? fallback : v;
    }

    private void show(String title, String body, boolean force) {
        NotificationManager nm =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        String channelId = force ? CHANNEL_FORCE : CHANNEL_DEFAULT;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = force
                    ? NotificationManager.IMPORTANCE_HIGH
                    : NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel ch = new NotificationChannel(
                    channelId,
                    force ? "Student Hub Important" : "Student Hub",
                    importance
            );
            ch.setDescription(force
                    ? "Important Student Hub app notifications"
                    : "Student Hub app notifications");
            nm.createNotificationChannel(ch);
        }

        Intent intent = new Intent(this, WebAppActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pi = PendingIntent.getActivity(
                this, 1001, intent,
                PendingIntent.FLAG_UPDATE_CURRENT |
                (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(getApplicationInfo().icon)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                        .setContentIntent(pi)
                        .setAutoCancel(true)
                        .setPriority(force
                                ? NotificationCompat.PRIORITY_HIGH
                                : NotificationCompat.PRIORITY_DEFAULT);

        nm.notify((int) (System.currentTimeMillis() & 0x7fffffff), builder.build());
    }
}
