package com.studenthub.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.HashMap;
import java.util.Map;

public class StudentHubMessagingService extends FirebaseMessagingService {
    private static final String CHANNEL_ID = "studenthub_messages";
    @Override public void onNewToken(String token) {
        super.onNewToken(token);
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Map<String,Object> data = new HashMap<>(); data.put("enabled", true); data.put("updatedAt", System.currentTimeMillis());
        FirebaseFirestore.getInstance().collection("Users").document(uid).collection("FcmTokens").document(token).set(data, SetOptions.merge());
    }
    @Override public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);
        String title = "StudentHub"; String body = "New notification";
        if (message.getNotification() != null) { if (message.getNotification().getTitle()!=null) title=message.getNotification().getTitle(); if (message.getNotification().getBody()!=null) body=message.getNotification().getBody(); }
        if (message.getData().get("title") != null) title=message.getData().get("title");
        if (message.getData().get("body") != null) body=message.getData().get("body");
        show(title, body);
    }
    private void show(String title,String body){
        NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26) nm.createNotificationChannel(new NotificationChannel(CHANNEL_ID,"StudentHub notifications",NotificationManager.IMPORTANCE_HIGH));
        Intent i=new Intent(this,MainActivity.class); i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi=PendingIntent.getActivity(this,0,i,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder b=new NotificationCompat.Builder(this,CHANNEL_ID).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title).setContentText(body).setStyle(new NotificationCompat.BigTextStyle().bigText(body)).setAutoCancel(true).setContentIntent(pi).setPriority(NotificationCompat.PRIORITY_HIGH);
        nm.notify((int)(System.currentTimeMillis()%100000),b.build());
    }
}
