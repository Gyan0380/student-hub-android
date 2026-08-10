package com.studenthub.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Launcher screen. Uses the SAME email convention as the Web App:
 * "{username}@studentchat.com" + password, against the SAME Firebase
 * project — so a StudentHub account works identically on web and Android.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText inputUsername, inputPassword;
    private Button buttonLogin;
    private TextView linkRegister;
    private ProgressBar progressBar;

    private FirebaseAuth auth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

        inputUsername = findViewById(R.id.inputUsername);
        inputPassword = findViewById(R.id.inputPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        linkRegister = findViewById(R.id.linkRegister);
        progressBar = findViewById(R.id.progressBar);

        buttonLogin.setOnClickListener(v -> attemptLogin());
        linkRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Already signed in from a previous session? Skip straight to Home.
        if (auth.getCurrentUser() != null) {
            goToHome();
        }
    }

    private void attemptLogin() {
        String username = inputUsername.getText().toString().trim().toLowerCase();
        String password = inputPassword.getText().toString();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Username aur password dono bharein.", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        String email = username + "@studentchat.com";

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> goToHome())
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Login fail: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void goToHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        buttonLogin.setEnabled(!loading);
    }
}
