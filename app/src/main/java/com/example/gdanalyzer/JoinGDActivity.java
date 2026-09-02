package com.example.gdanalyzer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class JoinGDActivity extends AppCompatActivity {

    private EditText etSessionId;
    private EditText etParticipantName;
    private TextView btnJoinGD;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_gdactivity);

        etSessionId = findViewById(R.id.etSessionId);
        etParticipantName = findViewById(R.id.etParticipantName);
        btnJoinGD = findViewById(R.id.btnJoinGD);

        db = FirebaseFirestore.getInstance();

        btnJoinGD.setOnClickListener(v -> joinDiscussion());
    }

    private void joinDiscussion() {

        String sessionId = etSessionId.getText().toString().trim();
        String participantName = etParticipantName.getText().toString().trim();

        if (sessionId.isEmpty()) {
            etSessionId.setError("Please enter Session ID");
            etSessionId.requestFocus();
            return;
        }

        if (participantName.isEmpty()) {
            etParticipantName.setError("Please enter your name");
            etParticipantName.requestFocus();
            return;
        }

        db.collection("discussions")
                .document(sessionId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (documentSnapshot.exists()) {

                        String participantId = com.google.firebase.auth.FirebaseAuth
                                .getInstance()
                                .getCurrentUser()
                                .getUid();

                        java.util.Map<String, Object> participant = new java.util.HashMap<>();
                        participant.put("name", participantName);
                        participant.put("userId", participantId);

                        db.collection("discussions")
                                .document(sessionId)
                                .collection("participants")
                                .document(participantId)
                                .set(participant)
                                .addOnSuccessListener(aVoid -> {

                                    Toast.makeText(
                                            JoinGDActivity.this,
                                            "Joined discussion successfully!",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    Intent intent = new Intent(JoinGDActivity.this, WaitingRoomActivity.class);
                                    intent.putExtra("sessionId", sessionId);
                                    intent.putExtra("participantName", participantName);
                                    startActivity(intent);
                                    finish();

                                })
                                .addOnFailureListener(e -> {

                                    Toast.makeText(
                                            JoinGDActivity.this,
                                            "Failed to join: " + e.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show();

                                });

                    }else {

                        Toast.makeText(
                                JoinGDActivity.this,
                                "Invalid Session ID",
                                Toast.LENGTH_LONG
                        ).show();
                    }

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            JoinGDActivity.this,
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();

                });
    }
}