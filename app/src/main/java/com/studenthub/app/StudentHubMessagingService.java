package com.studenthub.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class StudentHubMessagingService
        extends FirebaseMessagingService {

    private static final String GENERAL_CHANNEL =
            "student_hub_general";

    private static final String IMPORTANT_CHANNEL =
            "student_hub_important";

    @Override
    public void onCreate() {

        super.onCreate();

        createNotificationChannels();
    }

    @Override
    public void onNewToken(String token) {

        super.onNewToken(token);

        /*
         * IMPORTANT:
         *
         * Ye FCM token hai.
         *
         * Is token ko tumhare backend/database me
         * authenticated user ke saath save karna hoga.
         *
         * Token ko public mat karna.
         */
    }

    @Override
    public void onMessageReceived(
            RemoteMessage remoteMessage
    ) {

        String title = null;
        String body = null;
        String type = "message";
        String className = null;

        boolean mentioned = false;

        if (remoteMessage.getData() != null) {

            title =
                    remoteMessage
                            .getData()
                            .get("title");

            body =
                    remoteMessage
                            .getData()
                            .get("body");

            String receivedType =
                    remoteMessage
                            .getData()
                            .get("type");

            if (receivedType != null) {
                type = receivedType;
            }

            className =
                    remoteMessage
                            .getData()
                            .get("class");

            String mention =
                    remoteMessage
                            .getData()
                            .get("mentioned");

            mentioned =
                    "true".equalsIgnoreCase(
                            mention
                    );
        }

        if (title == null) {
            title = "Student Hub";
        }

        if (body == null) {
            body = "";
        }

        /*
         * IMPORTANT notification.
         */
        if ("force".equals(type)) {

            showNotification(
                    title,
                    body,
                    IMPORTANT_CHANNEL,
                    true
            );

            return;
        }

        boolean allowed =
                NotificationSettings.shouldShow(
                        this,
                        type,
                        className,
                        mentioned
                );

        if (!allowed) {
            return;
        }

        showNotification(
                title,
                body,
                GENERAL_CHANNEL,
                false
        );
    }

    private void createNotificationChannels() {

        if (Build.VERSION.SDK_INT <
                Build.VERSION_CODES.O) {

            return;
        }

        NotificationManager manager =
                getSystemService(
                        NotificationManager.class
                );

        if (manager == null) {
            return;
        }

        NotificationChannel general =
                new NotificationChannel(
                        GENERAL_CHANNEL,
                        "Student Hub",
                        NotificationManager
                                .IMPORTANCE_DEFAULT
                );

        general.setDescription(
                "Messages, mentions, replies and class notifications"
        );

        NotificationChannel important =
                new NotificationChannel(
                        IMPORTANT_CHANNEL,
                        "Important Student Hub",
                        NotificationManager
                                .IMPORTANCE_HIGH
                );

        important.setDescription(
                "Important Student Hub announcements"
        );

        manager.createNotificationChannel(
                general
        );

        manager.createNotificationChannel(
                important
        );
    }

    private void showNotification(
            String title,
            String body,
            String channelId,
            boolean important
    ) {

        Intent intent =
                new Intent(
                        this,
                        WebAppActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
        );

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        this,
                        (int)
                                System.currentTimeMillis(),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                |
                        PendingIntent.FLAG_IMMUTABLE
                );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(
                        this,
                        channelId
                );

        builder
                .setSmallIcon(
                        android.R.drawable
                                .ic_dialog_info
                )
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(
                        new NotificationCompat
                                .BigTextStyle()
                                .bigText(body)
                )
                .setAutoCancel(true)
                .setContentIntent(
                        pendingIntent
                );

        if (important) {

            builder.setPriority(
                    NotificationCompat
                            .PRIORITY_HIGH
            );
        }

        NotificationManager manager =
                (NotificationManager)
                        getSystemService(
                                NOTIFICATION_SERVICE
                        );

        if (manager != null) {

            manager.notify(
                    (int)
                            System.currentTimeMillis(),
                    builder.build()
            );
        }
    }
    }
