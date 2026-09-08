package com.example.gdanalyzer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;
    private TextView btnLogin;
    private TextView tvRegister;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent incomingIntent = getIntent();

        if (Intent.ACTION_VIEW.equals(incomingIntent.getAction())
                && incomingIntent.getData() != null) {

            String sessionId =
                    incomingIntent.getData().getLastPathSegment();

            if (sessionId != null && !sessionId.isEmpty()) {
                incomingIntent.putExtra("sessionId", sessionId);
            }
        }

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);

        firebaseAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnLogin.setOnClickListener(v -> {
            Toast.makeText(
                    MainActivity.this,
                    "Login button clicked",
                    Toast.LENGTH_SHORT
            ).show();

            validateLogin();
        });
        tvRegister.setOnClickListener(v -> {
            Intent registerIntent = new Intent(
                    MainActivity.this,
                    RegisterActivity.class
            );
            startActivity(registerIntent);
        });
    }

    private void validateLogin() {

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (email.isEmpty()) {
            etEmail.setError("Please enter your email address");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Please enter your password");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        loginWithFirebase(email, password);
    }

    private void loginWithFirebase(String email, String password) {

        btnLogin.setEnabled(false);

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    btnLogin.setEnabled(true);

                    if (task.isSuccessful()) {

                        Toast.makeText(
                                MainActivity.this,
                                "Firebase login successful",
                                Toast.LENGTH_SHORT
                        ).show();

                        Toast.makeText(
                                MainActivity.this,
                                "Login successful!",
                                Toast.LENGTH_SHORT
                        ).show();

                        String sessionId =
                                getIntent().getStringExtra("sessionId");

                        if (sessionId != null && !sessionId.isEmpty()) {

                            joinDiscussionFromLink(sessionId);

                        } else {

                            Intent hostIntent = new Intent(
                                    MainActivity.this,
                                    DashboardActivity.class
                            );

                            startActivity(hostIntent);
                            finish();
                        }

                    } else {

                        String errorMessage =
                                "Login failed. Please check your details.";

                        if (task.getException() != null) {
                            errorMessage =
                                    task.getException().getMessage();
                        }

                        Toast.makeText(
                                MainActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void joinDiscussionFromLink(String sessionId) {

        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(
                    MainActivity.this,
                    "User login information not found.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        String userId =
                firebaseAuth.getCurrentUser().getUid();

        String email =
                firebaseAuth.getCurrentUser().getEmail();

        db.collection("discussions")
                .document(sessionId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (!documentSnapshot.exists()) {

                        Toast.makeText(
                                MainActivity.this,
                                "Discussion not found.",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    Long limitValue =
                            documentSnapshot.getLong("participants");

                    final long participantLimit =
                            limitValue != null ? limitValue : 10L;

                    db.collection("discussions")
                            .document(sessionId)
                            .collection("participants")
                            .document(userId)
                            .get()
                            .addOnSuccessListener(participantDocument -> {

                                // User has already joined
                                if (participantDocument.exists()) {

                                    openParticipantWaitingRoom(sessionId);
                                    return;
                                }

                                db.collection("discussions")
                                        .document(sessionId)
                                        .collection("participants")
                                        .get()
                                        .addOnSuccessListener(participantSnapshot -> {

                                            int currentCount =
                                                    participantSnapshot.size();

                                            if (currentCount >= participantLimit) {

                                                Toast.makeText(
                                                        MainActivity.this,
                                                        "Meeting is full. Maximum "
                                                                + participantLimit
                                                                + " participants are allowed.",
                                                        Toast.LENGTH_LONG
                                                ).show();

                                                return;
                                            }

                                            Map<String, Object> participant =
                                                    new HashMap<>();

                                            participant.put(
                                                    "userId",
                                                    userId
                                            );

                                            participant.put(
                                                    "name",
                                                    email != null
                                                            ? email
                                                            : "Participant"
                                            );

                                            participant.put(
                                                    "joinedAt",
                                                    com.google.firebase.firestore.FieldValue
                                                            .serverTimestamp()
                                            );

                                            db.collection("discussions")
                                                    .document(sessionId)
                                                    .collection("participants")
                                                    .document(userId)
                                                    .set(participant)
                                                    .addOnSuccessListener(aVoid -> {

                                                        openParticipantWaitingRoom(
                                                                sessionId
                                                        );

                                                    })
                                                    .addOnFailureListener(e -> {

                                                        Toast.makeText(
                                                                MainActivity.this,
                                                                "Failed to join discussion: "
                                                                        + e.getMessage(),
                                                                Toast.LENGTH_LONG
                                                        ).show();

                                                    });

                                        })
                                        .addOnFailureListener(e -> {

                                            Toast.makeText(
                                                    MainActivity.this,
                                                    "Failed to check participants: "
                                                            + e.getMessage(),
                                                    Toast.LENGTH_LONG
                                            ).show();

                                        });

                            })
                            .addOnFailureListener(e -> {

                                Toast.makeText(
                                        MainActivity.this,
                                        "Failed to check participant: "
                                                + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();

                            });

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            MainActivity.this,
                            "Failed to load discussion: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();

                });
    }

    private void openParticipantWaitingRoom(String sessionId) {

        Intent participantIntent = new Intent(
                MainActivity.this,
                WaitingRoomActivity.class
        );

        participantIntent.putExtra(
                "sessionId",
                sessionId
        );

        startActivity(participantIntent);

        finish();
    }
}