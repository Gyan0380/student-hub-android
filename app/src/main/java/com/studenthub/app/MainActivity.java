package com.studenthub.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.studenthub.app.adapter.MessageAdapter;
import com.studenthub.app.model.ChatMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Pure native Android chat screen — NO WebView anywhere.
 *
 * How the real-time cross-platform sync works:
 *  1. This app and the Web App both connect to the SAME Firebase project's
 *     Realtime Database, under the same node: "messages".
 *  2. Sending a message = pushing a new child under "messages" with push().setValue(...).
 *  3. Receiving messages = attaching a ValueEventListener to "messages", which Firebase
 *     fires immediately with the current data AND again every single time ANY client
 *     (Android or Web) adds/changes/removes a message — no polling required.
 *
 * Because both platforms listen to the same node, a message sent from the browser
 * appears here within a fraction of a second, and vice-versa.
 */
public class MainActivity extends AppCompatActivity {

    // Root node in the Realtime Database that stores every chat message.
    // The Web App MUST read/write this exact same path: /messages
    private static final String MESSAGES_PATH = "messages";

    private static final String PREFS_NAME = "student_hub_prefs";
    private static final String KEY_SENDER_ID = "sender_id";
    private static final String KEY_SENDER_NAME = "sender_name";

    private RecyclerView recyclerViewMessages;
    private EditText editTextMessage;
    private ImageButton buttonSend;
    private MessageAdapter messageAdapter;

    private DatabaseReference messagesRef;
    private ValueEventListener messagesListener;

    private String senderId;
    private String senderName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Step 1: figure out who "we" are on this device so we can tell our own
        // messages apart from everyone else's (for right vs left bubble alignment).
        setupIdentity();

        // Step 2: wire up UI views.
        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);

        messageAdapter = new MessageAdapter(senderId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerViewMessages.setLayoutManager(layoutManager);
        recyclerViewMessages.setAdapter(messageAdapter);

        // Step 3: initialize Firebase Realtime Database reference.
        // FirebaseApp.initializeApp() is called automatically at app startup because
        // the google-services.json config file + google-services Gradle plugin are present.
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        messagesRef = database.getReference(MESSAGES_PATH);

        // Optional: anonymous sign-in so senderId can later be swapped for
        // FirebaseAuth.getCurrentUser().getUid() if you want auth-backed identity
        // shared with the Web App. Safe to ignore failures — chat still works
        // using the locally generated senderId.
        FirebaseAuth.getInstance().signInAnonymously();

        buttonSend.setOnClickListener(v -> sendMessage());
    }

    @Override
    protected void onStart() {
        super.onStart();
        attachMessagesListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Always detach listeners when the screen isn't visible to avoid leaks
        // and unnecessary background reads.
        if (messagesRef != null && messagesListener != null) {
            messagesRef.removeEventListener(messagesListener);
        }
    }

    /**
     * Loads (or creates) a persistent anonymous identity for this device, plus
     * asks the user for a display name the very first time the app is opened.
     */
    private void setupIdentity() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        senderId = prefs.getString(KEY_SENDER_ID, null);
        senderName = prefs.getString(KEY_SENDER_NAME, null);

        if (senderId == null) {
            senderId = UUID.randomUUID().toString();
            prefs.edit().putString(KEY_SENDER_ID, senderId).apply();
        }

        if (senderName == null) {
            // Ask for a display name on first launch only.
            final EditText nameInput = new EditText(this);
            nameInput.setHint("e.g. Gyan");
            new AlertDialog.Builder(this)
                    .setTitle("Enter your name")
                    .setMessage("This name will be shown to others in the chat.")
                    .setView(nameInput)
                    .setCancelable(false)
                    .setPositiveButton("Continue", (dialog, which) -> {
                        String name = nameInput.getText().toString().trim();
                        senderName = TextUtils.isEmpty(name) ? "Guest" : name;
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                                .edit()
                                .putString(KEY_SENDER_NAME, senderName)
                                .apply();
                    })
                    .show();
            // Fallback default while the dialog is still showing.
            if (senderName == null) senderName = "Guest";
        }
    }

    /**
     * Attaches a ValueEventListener to the "messages" node. This fires once
     * immediately with all existing messages, and then automatically again
     * every time data changes on Firebase's servers — from THIS app or from
     * the Web App — giving true real-time sync with zero polling.
     */
    private void attachMessagesListener() {
        messagesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ChatMessage> messages = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    ChatMessage message = child.getValue(ChatMessage.class);
                    if (message != null) {
                        messages.add(message);
                    }
                }

                // Firebase Realtime Database returns children ordered by key by
                // default (push keys are chronologically sortable), so the list
                // is already in send order — oldest first.
                messageAdapter.setMessages(messages);

                if (!messages.isEmpty()) {
                    recyclerViewMessages.scrollToPosition(messages.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this,
                        "Failed to load messages: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        };

        // Only fetch/listen to the last 200 messages to keep things fast and light.
        messagesRef.limitToLast(200).addValueEventListener(messagesListener);
    }

    /**
     * Pushes a new message under /messages in Firebase Realtime Database.
     * push() generates a unique, chronologically-ordered key (e.g. -Nabc123...),
     * so we never overwrite another message — from this device OR the Web App.
     */
    private void sendMessage() {
        String text = editTextMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            return;
        }

        ChatMessage message = new ChatMessage(
                senderId,
                senderName,
                text,
                System.currentTimeMillis()
        );

        // Generates a new unique child key under "messages" and writes the message there.
        DatabaseReference newMessageRef = messagesRef.push();
        newMessageRef.setValue(message)
                .addOnSuccessListener(unused -> {
                    // Clear the input only after a confirmed successful write.
                    editTextMessage.setText("");
                })
                .addOnFailureListener(e -> Toast.makeText(
                        MainActivity.this,
                        "Message failed to send: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
    }
}
