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

                    if (!documentSnapshot.exists()) {

                        Toast.makeText(
                                JoinGDActivity.this,
                                "Invalid Session ID",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    // Get maximum number of participants
                    Long participantLimit =
                            documentSnapshot.getLong("participants");

                    if (participantLimit == null) {

                        Toast.makeText(
                                JoinGDActivity.this,
                                "Participant limit not found",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    String participantId = com.google.firebase.auth.FirebaseAuth
                            .getInstance()
                            .getCurrentUser()
                            .getUid();

                    // Check whether this participant has already joined
                    db.collection("discussions")
                            .document(sessionId)
                            .collection("participants")
                            .document(participantId)
                            .get()
                            .addOnSuccessListener(existingParticipant -> {

                                if (existingParticipant.exists()) {

                                    // Already joined - allow them to continue
                                    openWaitingRoom(sessionId, participantName);

                                    return;
                                }

                                // Count current participants
                                db.collection("discussions")
                                        .document(sessionId)
                                        .collection("participants")
                                        .get()
                                        .addOnSuccessListener(participantSnapshot -> {

                                            int currentParticipants =
                                                    participantSnapshot.size();

                                            if (currentParticipants >= participantLimit) {

                                                Toast.makeText(
                                                        JoinGDActivity.this,
                                                        "Meeting is full. Maximum "
                                                                + participantLimit
                                                                + " participants are allowed.",
                                                        Toast.LENGTH_LONG
                                                ).show();

                                                return;
                                            }

                                            // Add new participant
                                            java.util.Map<String, Object> participant =
                                                    new java.util.HashMap<>();

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

                                                        openWaitingRoom(
                                                                sessionId,
                                                                participantName
                                                        );
                                                    })
                                                    .addOnFailureListener(e -> {

                                                        Toast.makeText(
                                                                JoinGDActivity.this,
                                                                "Failed to join: "
                                                                        + e.getMessage(),
                                                                Toast.LENGTH_LONG
                                                        ).show();
                                                    });

                                        })
                                        .addOnFailureListener(e -> {

                                            Toast.makeText(
                                                    JoinGDActivity.this,
                                                    "Unable to check participants: "
                                                            + e.getMessage(),
                                                    Toast.LENGTH_LONG
                                            ).show();
                                        });

                            })
                            .addOnFailureListener(e -> {

                                Toast.makeText(
                                        JoinGDActivity.this,
                                        "Unable to check participant: "
                                                + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            });

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            JoinGDActivity.this,
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
    private void openWaitingRoom(String sessionId, String participantName) {

        Intent intent = new Intent(
                JoinGDActivity.this,
                WaitingRoomActivity.class
        );

        intent.putExtra("sessionId", sessionId);
        intent.putExtra("participantName", participantName);

        startActivity(intent);
        finish();
    }
}