package com.example.gdanalyzer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import androidx.appcompat.app.AppCompatActivity;

public class CreateGDActivity extends AppCompatActivity {

    private EditText etTopic;
    private EditText etParticipants;
    private EditText etDuration;
    private TextView btnCreateGD;
    private FirebaseFirestore db;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_gdactivity);
        db = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        etTopic = findViewById(R.id.etTopic);
        etParticipants = findViewById(R.id.etParticipants);
        etDuration = findViewById(R.id.etDuration);
        btnCreateGD = findViewById(R.id.btnCreateGD);

        btnCreateGD.setOnClickListener(v -> createDiscussion());
    }

    private void createDiscussion() {

        String topic = etTopic.getText().toString().trim();
        String participants = etParticipants.getText().toString().trim();
        String duration = etDuration.getText().toString().trim();

        if (topic.isEmpty()) {
            etTopic.setError("Please enter a discussion topic");
            etTopic.requestFocus();
            return;
        }

        if (participants.isEmpty()) {
            etParticipants.setError("Please enter number of participants");
            etParticipants.requestFocus();
            return;
        }

        int participantCount = Integer.parseInt(participants);

        if (participantCount < 2 || participantCount > 10) {
            etParticipants.setError("Participants must be between 2 and 10");
            etParticipants.requestFocus();
            return;
        }

        if (duration.isEmpty()) {
            etDuration.setError("Please enter discussion duration");
            etDuration.requestFocus();
            return;
        }

        int durationMinutes = Integer.parseInt(duration);

        if (durationMinutes < 1 || durationMinutes > 60) {
            etDuration.setError("Duration must be between 1 and 60 minutes");
            etDuration.requestFocus();
            return;
        }

        saveDiscussion(topic, participantCount, durationMinutes);
    }

    private void saveDiscussion(String topic, int participantCount, int durationMinutes) {

        String userId = firebaseAuth.getCurrentUser().getUid();

        java.util.Map<String, Object> discussion = new java.util.HashMap<>();

        discussion.put("topic", topic);
        discussion.put("participants", participantCount);
        discussion.put("duration", durationMinutes);
        discussion.put("hostId", userId);
        discussion.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection("discussions")
                .add(discussion)
                .addOnSuccessListener(documentReference -> {

                    String sessionId = documentReference.getId();

                    Intent intent = new Intent(CreateGDActivity.this, HostWaitingRoomActivity.class);
                    intent.putExtra("sessionId", sessionId);
                    startActivity(intent);
                    finish();

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            CreateGDActivity.this,
                            "Failed to create discussion: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();

                });
    }
}