package com.studenthub.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates the SAME Firebase Auth account + Users/{uid} Firestore document
 * shape as the Web App's renderRegister(), so an account made here also
 * works when logging into the website (and vice-versa).
 */
public class RegisterActivity extends AppCompatActivity {

    private static final String DEFAULT_PHOTO = "https://cdn-icons-png.flaticon.com/512/149/149071.png";

    private EditText inputFullName, inputUsername, inputPassword, inputDob, inputSchool;
    private Spinner spinnerClass;
    private Button buttonRegister;
    private TextView linkLogin;
    private ProgressBar progressBar;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        inputFullName = findViewById(R.id.inputFullName);
        inputUsername = findViewById(R.id.inputUsername);
        inputPassword = findViewById(R.id.inputPassword);
        inputDob = findViewById(R.id.inputDob);
        inputSchool = findViewById(R.id.inputSchool);
        spinnerClass = findViewById(R.id.spinnerClass);
        buttonRegister = findViewById(R.id.buttonRegister);
        linkLogin = findViewById(R.id.linkLogin);
        progressBar = findViewById(R.id.progressBar);

        List<String> classes = new ArrayList<>();
        classes.add("-- Select Class --");
        for (int i = 1; i <= 12; i++) classes.add("Class " + i);
        classes.add("12th Pass / College");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, classes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerClass.setAdapter(adapter);

        buttonRegister.setOnClickListener(v -> attemptRegister());
        linkLogin.setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        String fullName = inputFullName.getText().toString().trim();
        String username = inputUsername.getText().toString().trim().toLowerCase();
        String password = inputPassword.getText().toString();
        String dob = inputDob.getText().toString().trim();
        String classLevel = spinnerClass.getSelectedItemPosition() > 0
                ? spinnerClass.getSelectedItem().toString() : "";
        String schoolName = inputSchool.getText().toString().trim();

        if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(username) || TextUtils.isEmpty(password)
                || TextUtils.isEmpty(dob) || TextUtils.isEmpty(classLevel) || TextUtils.isEmpty(schoolName)) {
            Toast.makeText(this, "Sab fields bharna zaroori hai.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Password kam se kam 6 characters ka hona chahiye.", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        String email = username + "@studentchat.com";

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) {
                        setLoading(false);
                        return;
                    }

                    Map<String, Object> userDoc = new HashMap<>();
                    userDoc.put("uid", user.getUid());
                    userDoc.put("fullName", fullName);
                    userDoc.put("username", username);
                    userDoc.put("dob", dob);
                    userDoc.put("classLevel", classLevel);
                    userDoc.put("schoolName", schoolName);
                    userDoc.put("profilePhoto", DEFAULT_PHOTO);
                    userDoc.put("bio", "");
                    userDoc.put("role", "Student");
                    List<String> classAccess = new ArrayList<>();
                    classAccess.add(classLevel);
                    userDoc.put("classAccess", classAccess);
                    userDoc.put("createdAt", FieldValue.serverTimestamp());
                    userDoc.put("isBanned", false);
                    userDoc.put("timeoutExpiry", null);

                    db.collection("Users").document(user.getUid()).set(userDoc)
                            .addOnSuccessListener(unused -> {
                                setLoading(false);
                                startActivity(new Intent(this, HomeActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                Toast.makeText(this, "Profile save nahi hua: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    String msg = e.getMessage() != null && e.getMessage().contains("already in use")
                            ? "Yeh username pehle se liya gaya hai." : e.getMessage();
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        buttonRegister.setEnabled(!loading);
    }
}
