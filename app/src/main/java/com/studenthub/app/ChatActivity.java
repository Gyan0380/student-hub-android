package com.studenthub.app;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.studenthub.app.adapter.MessageAdapter;
import com.studenthub.app.model.ChatMessage;
import com.studenthub.app.model.UserModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Native chat screen — NO WebView anywhere.
 *
 * How the real-time cross-platform sync works:
 *  1. This app and the Web App both connect to the SAME Firebase project's
 *     Firestore database, at the SAME path: Chats/{roomId}/Messages.
 *  2. Sending a message = addDoc() with the same field names the Web App
 *     uses (senderId, senderName, senderPhoto, text, createdAt).
 *  3. Receiving messages = a Firestore snapshot listener on that path, which
 *     fires immediately with current data and again the instant ANY client
 *     (Android or Web) adds a message — no polling required.
 *
 * Because both platforms read/write the exact same collection, a message
 * sent from the browser appears here within a fraction of a second — like
 * Discord's cross-platform sync — and vice-versa.
 */
public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_ROOM_ID = "extra_room_id";
    public static final String EXTRA_ROOM_TITLE = "extra_room_title";

    private RecyclerView recyclerViewMessages;
    private EditText editTextMessage;
    private ImageButton buttonSend;
    private MessageAdapter messageAdapter;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration messagesListener;

    private String roomId;
    private String roomTitle;
    private boolean isAnonymous;

    private UserModel currentUser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        roomId = getIntent().getStringExtra(EXTRA_ROOM_ID);
        roomTitle = getIntent().getStringExtra(EXTRA_ROOM_TITLE);
        if (TextUtils.isEmpty(roomId)) roomId = "global";
        isAnonymous = roomId.toLowerCase().contains("anonymous");

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(!TextUtils.isEmpty(roomTitle) ? roomTitle : "Chat");
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);

        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        messageAdapter = new MessageAdapter(uid);
        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMessages.setAdapter(messageAdapter);

        buttonSend.setOnClickListener(v -> sendMessage());

        loadCurrentUser();
    }

    private void loadCurrentUser() {
        if (auth.getCurrentUser() == null) return;
        db.collection("Users").document(auth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(snap -> {
                    currentUser = snap.toObject(UserModel.class);
                    if (currentUser != null && currentUser.getUid() == null) {
                        currentUser.setUid(snap.getId());
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        attachMessagesListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (messagesListener != null) {
            messagesListener.remove();
            messagesListener = null;
        }
    }

    /**
     * Same collection path as the Web App: Chats/{roomId}/Messages,
     * ordered by createdAt ascending — identical to app.js's renderChat().
     */
    private void attachMessagesListener() {
        Query query = db.collection("Chats").document(roomId).collection("Messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .limitToLast(200);

        messagesListener = query.addSnapshotListener((QuerySnapshot snapshots, com.google.firebase.firestore.FirebaseFirestoreException error) -> {
            if (error != null) {
                Toast.makeText(this, "Failed to load messages: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            if (snapshots == null) return;

            List<ChatMessage> messages = new ArrayList<>();
            snapshots.forEach(doc -> {
                ChatMessage m = doc.toObject(ChatMessage.class);
                m.setId(doc.getId());
                messages.add(m);
            });

            messageAdapter.setMessages(messages);
            if (!messages.isEmpty()) {
                recyclerViewMessages.scrollToPosition(messages.size() - 1);
            }
        });
    }

    private void sendMessage() {
        String text = editTextMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Login required.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        String senderName = isAnonymous ? "Anonymous Ninja"
                : (currentUser != null && currentUser.getUsername() != null ? currentUser.getUsername() : "Student");
        String senderPhoto = isAnonymous ? "https://cdn-icons-png.flaticon.com/512/1752/1752184.png"
                : (currentUser != null && currentUser.getProfilePhoto() != null ? currentUser.getProfilePhoto()
                   : "https://cdn-icons-png.flaticon.com/512/149/149071.png");

        Map<String, Object> message = new HashMap<>();
        message.put("text", text);
        message.put("photos", null);
        message.put("photoUrl", null);
        message.put("senderId", uid);
        message.put("senderName", senderName);
        message.put("senderPhoto", senderPhoto);
        message.put("createdAt", FieldValue.serverTimestamp());
        message.put("replyTo", null);
        message.put("edited", false);

        db.collection("Chats").document(roomId).collection("Messages").add(message)
                .addOnSuccessListener(ref -> editTextMessage.setText(""))
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Message failed to send: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
