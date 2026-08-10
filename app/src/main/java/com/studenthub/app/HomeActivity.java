package com.studenthub.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import android.content.pm.PackageManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.studenthub.app.model.UserModel;
import com.studenthub.app.util.Slug;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The options / home menu — native equivalent of the Web App's renderHome().
 * Same option list, same destinations, same theme colors.
 */
public class HomeActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private LinearLayout optionsContainer;
    private TextView textFullName, textSchool, textAvatarInitial;
    private UserModel currentUser;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 2001;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        optionsContainer = findViewById(R.id.optionsContainer);
        textFullName = findViewById(R.id.textFullName);
        textSchool = findViewById(R.id.textSchool);
        textAvatarInitial = findViewById(R.id.textAvatarInitial);

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        requestNotificationPermissionIfNeeded();
        loadUserAndBuildMenu();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST);
        }
    }

    private void registerFcmToken() {
        if (auth.getCurrentUser() == null) return;
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(this::saveFcmToken)
                .addOnFailureListener(e -> { /* non-fatal */ });
    }

    private void saveFcmToken(String token) {
        if (auth.getCurrentUser() == null || token == null || token.isEmpty()) return;
        String uid = auth.getCurrentUser().getUid();
        String tokenId = token.replaceAll("[^A-Za-z0-9_-]", "_");
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("enabled", true);
        data.put("platform", "android");
        data.put("updatedAt", FieldValue.serverTimestamp());
        db.collection("Users").document(uid).collection("fcmTokens").document(tokenId).set(data);
    }

    private void loadUserAndBuildMenu() {
        if (auth.getCurrentUser() == null) {
            goToLogin();
            return;
        }
        String uid = auth.getCurrentUser().getUid();
        db.collection("Users").document(uid).get()
                .addOnSuccessListener(this::onUserLoaded)
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Profile load nahi hua: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void onUserLoaded(DocumentSnapshot snap) {
        if (!snap.exists()) {
            goToLogin();
            return;
        }
        currentUser = snap.toObject(UserModel.class);
        if (currentUser == null) currentUser = new UserModel();
        if (currentUser.getUid() == null) currentUser.setUid(snap.getId());

        String name = !TextUtils.isEmpty(currentUser.getFullName()) ? currentUser.getFullName() : "Student";
        textFullName.setText(name);
        textSchool.setText(!TextUtils.isEmpty(currentUser.getSchoolName()) ? currentUser.getSchoolName() : "Not Provided");
        textAvatarInitial.setText(String.valueOf(Character.toUpperCase(name.charAt(0))));

        registerFcmToken();
        buildOptions();
    }

    private void buildOptions() {
        optionsContainer.removeAllViews();

        addOption("🌍", "Global Chat", "Sab verified students ke saath", R.color.ic_blue,
                v -> openChat("global", "Global Chat"));

        addOption("🥷", "Anonymous Chat", "Naam/DP hidden rehta hai", R.color.ic_purple,
                v -> openChat("anonymous", "Anonymous Chat"));

        List<String> classAccess = currentUser.classAccessOrDefault();
        for (String c : classAccess) {
            String slug = Slug.slugify(c);
            addOption("🎓", c + " Room", "Sirf is class ke students", R.color.ic_green,
                    v -> openChat(slug, c + " Room"));
        }

        addOption("🔔", "Notifications", "Web + Android synced notifications", R.color.ic_blue,
                v -> startActivity(new Intent(this, NotificationsActivity.class)));

        addOption("📜", "Community Rules", "Chat guidelines padhein", R.color.ic_amber,
                v -> showRules());

        addOption("💡", "Suggestion Box", "Apna suggestion Admin tak pahunchayein", R.color.ic_cyan,
                v -> showFeedbackDialog("Suggestions", "Suggestion Box", "Apna suggestion likhein..."));

        addOption("🐛", "Report a Bug", "Bug ka detail batayein", R.color.ic_red,
                v -> showFeedbackDialog("BugReports", "Report a Bug", "Bug kya hua, describe karein..."));

        addOption("⚙️", "Settings", "Profile edit, theme, logout", R.color.ic_blue,
                v -> startActivity(new Intent(this, ProfileActivity.class)));

        if (currentUser.isAdminOrOwner()) {
            addOption("🛡️", "Admin Chat Room", "Sirf Admin/Owner ke liye", R.color.ic_red,
                    v -> openChat("admin-room", "Admin Chat Room"));
            addOption("📢", "Send Announcement", "Web + Android users ko notification", R.color.ic_amber,
                    v -> showAnnouncementDialog());
        }
    }

    private void addOption(String emoji, String title, String subtitle, int iconBgColorRes, View.OnClickListener onClick) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_option, optionsContainer, false);
        ((TextView) row.findViewById(R.id.optionIcon)).setText(emoji);
        ((TextView) row.findViewById(R.id.optionTitle)).setText(title);
        ((TextView) row.findViewById(R.id.optionSubtitle)).setText(subtitle);
        row.findViewById(R.id.iconWrap).setBackgroundTintList(
                ContextCompat.getColorStateList(this, iconBgColorRes));
        row.setOnClickListener(onClick);
        optionsContainer.addView(row);
    }

    private void openChat(String roomId, String roomTitle) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_ROOM_ID, roomId);
        intent.putExtra(ChatActivity.EXTRA_ROOM_TITLE, roomTitle);
        startActivity(intent);
    }

    private void showRules() {
        AlertDialog loading = new AlertDialog.Builder(this)
                .setTitle("📜 Community Rules")
                .setMessage("Loading...")
                .setPositiveButton("Close", null)
                .show();
        db.collection("Settings").document("CommunityRules").get()
                .addOnSuccessListener(snap -> {
                    String rules = snap.exists() ? snap.getString("rules") : null;
                    if (TextUtils.isEmpty(rules)) {
                        rules = "1. Respect every student.\n2. No abuse, spam, or bullying.\n3. Follow Admin instructions.";
                    }
                    loading.setMessage(rules);
                })
                .addOnFailureListener(e -> loading.setMessage("Rules load nahi hui: " + e.getMessage()));
    }

    private void showFeedbackDialog(String collectionName, String title, String hint) {
        FrameLayout wrap = new FrameLayout(this);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        wrap.setPadding(pad, pad, pad, pad);
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setMinLines(3);
        wrap.addView(input);

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(wrap)
                .setPositiveButton("Send", (dialog, which) -> {
                    String text = input.getText().toString().trim();
                    if (TextUtils.isEmpty(text)) return;
                    Map<String, Object> doc = new HashMap<>();
                    doc.put("text", text);
                    doc.put("uid", currentUser.getUid());
                    doc.put("username", currentUser.getUsername());
                    doc.put("createdAt", FieldValue.serverTimestamp());
                    db.collection(collectionName).add(doc)
                            .addOnSuccessListener(ref -> Toast.makeText(this, "Bhej diya! Dhanyavaad 🙌", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Send nahi hua: " + e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    private void showAnnouncementDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad, pad, 0);
        EditText title = new EditText(this); title.setHint("Title"); box.addView(title);
        EditText body = new EditText(this); body.setHint("Announcement / notification"); body.setMinLines(4); box.addView(body);
        new AlertDialog.Builder(this).setTitle("📢 Send Announcement").setView(box)
            .setPositiveButton("Send", (d,w) -> {
                String t=title.getText().toString().trim(), b=body.getText().toString().trim();
                if(TextUtils.isEmpty(t)||TextUtils.isEmpty(b)){Toast.makeText(this,"Title aur message dono bharein.",Toast.LENGTH_SHORT).show();return;}
                Map<String,Object> n=new HashMap<>();
                n.put("title",t); n.put("body",b); n.put("text",b); n.put("toUid","all");
                n.put("senderUid",auth.getCurrentUser().getUid()); n.put("senderRole",currentUser.getRole());
                n.put("createdAt",FieldValue.serverTimestamp());
                db.collection("Notifications").add(n).addOnSuccessListener(x -> Toast.makeText(this,"Notification sent — web aur Android notification feed dono sync ho gaye.",Toast.LENGTH_LONG).show())
                  .addOnFailureListener(e -> Toast.makeText(this,"Send failed: "+e.getMessage(),Toast.LENGTH_LONG).show());
            }).setNegativeButton("Cancel",null).show();
    }

    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
