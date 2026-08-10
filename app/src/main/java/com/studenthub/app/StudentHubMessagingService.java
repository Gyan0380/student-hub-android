package com.studenthub.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * Receives FCM messages and displays a real Android OS notification.
 * The Cloud Function sends FCM after a Notifications/{id} document is created.
 */
public class StudentHubMessagingService extends FirebaseMessagingService {
    private static final String CHANNEL_ID = "student_hub_general";

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        String title = message.getNotification() != null && message.getNotification().getTitle() != null
                ? message.getNotification().getTitle() : message.getData().get("title");
        String body = message.getNotification() != null && message.getNotification().getBody() != null
                ? message.getNotification().getBody() : message.getData().get("body");

        if (title == null) title = "Student Hub";
        if (body == null) body = "";

        Intent intent = new Intent(this, NotificationsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 1001, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat.from(this)
                    .notify((int) (System.currentTimeMillis() & 0x7fffffff), builder.build());
        } catch (SecurityException ignored) {
            // Android 13+: permission is requested by HomeActivity.
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        saveToken(token);
    }

    private void saveToken(String token) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String tokenId = token.replaceAll("[^A-Za-z0-9_-]", "_");
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("enabled", true);
        data.put("platform", "android");
        data.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance()
                .collection("Users").document(uid)
                .collection("fcmTokens").document(tokenId)
                .set(data);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Student Hub notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Student Hub announcements and alerts");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }
}
