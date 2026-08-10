package com.studenthub.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Settings / Options screen — native equivalent of the Web App's Edit
 * Profile page, plus a theme toggle (same idea as the website's
 * data-theme switch) and logout.
 */
public class ProfileActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "student_hub_prefs";
    private static final String KEY_DARK_MODE = "dark_mode";

    private EditText inputBio, inputSchool;
    private Switch switchDarkMode;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        inputBio = findViewById(R.id.inputBio);
        inputSchool = findViewById(R.id.inputSchool);
        switchDarkMode = findViewById(R.id.switchDarkMode);
        Button buttonSave = findViewById(R.id.buttonSaveProfile);
        Button buttonLogout = findViewById(R.id.buttonLogout);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        switchDarkMode.setChecked(prefs.getBoolean(KEY_DARK_MODE, false));
        switchDarkMode.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean(KEY_DARK_MODE, isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(isChecked
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_NO);
        });

        buttonSave.setOnClickListener(v -> saveProfile());
        buttonLogout.setOnClickListener(v -> logout());

        loadProfile();
    }

    private void loadProfile() {
        if (auth.getCurrentUser() == null) return;
        db.collection("Users").document(auth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(this::onProfileLoaded)
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Profile load nahi hua: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void onProfileLoaded(DocumentSnapshot snap) {
        if (!snap.exists()) return;
        String bio = snap.getString("bio");
        String school = snap.getString("schoolName");
        inputBio.setText(bio != null ? bio : "");
        inputSchool.setText(school != null ? school : "");
    }

    private void saveProfile() {
        if (auth.getCurrentUser() == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("bio", inputBio.getText().toString().trim());
        updates.put("schoolName", inputSchool.getText().toString().trim());

        db.collection("Users").document(auth.getCurrentUser().getUid())
                .update(updates)
                .addOnSuccessListener(unused -> Toast.makeText(this, "Profile updated ✅", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Save nahi hua: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void logout() {
        auth.signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
