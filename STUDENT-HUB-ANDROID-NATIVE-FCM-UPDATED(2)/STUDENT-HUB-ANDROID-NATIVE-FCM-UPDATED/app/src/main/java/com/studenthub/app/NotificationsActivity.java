package com.studenthub.app;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.studenthub.app.adapter.NotificationAdapter;
import com.studenthub.app.model.AppNotification;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration listener;
    private NotificationAdapter adapter;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("🔔 Notifications");
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        RecyclerView rv = findViewById(R.id.recyclerNotifications);
        TextView empty = findViewById(R.id.emptyNotifications);
        adapter = new NotificationAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
        listen(empty);
    }

    private void listen(TextView empty) {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();
        Query q = db.collection("Notifications")
                .whereIn("toUid", java.util.Arrays.asList("all", uid))
                .limit(100);
        listener = q.addSnapshotListener((snap, e) -> {
            if (e != null) {
                Toast.makeText(this, "Notifications load nahi hui: " + e.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }
            List<AppNotification> list = new ArrayList<>();
            if (snap != null) for (DocumentSnapshot d : snap.getDocuments()) {
                AppNotification n = d.toObject(AppNotification.class);
                if (n != null) { n.setId(d.getId()); list.add(n); }
            }
            adapter.setItems(list);
            empty.setVisibility(list.isEmpty() ? TextView.VISIBLE : TextView.GONE);
        });
    }

    @Override protected void onStop() {
        super.onStop();
        if (listener != null) { listener.remove(); listener = null; }
    }
}
