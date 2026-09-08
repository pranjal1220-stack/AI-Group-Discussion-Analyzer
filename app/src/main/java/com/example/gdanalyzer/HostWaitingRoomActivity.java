package com.example.gdanalyzer;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class HostWaitingRoomActivity extends AppCompatActivity {

    private TextView tvSessionId;
    private TextView tvTopic;
    private TextView tvParticipants;
    private TextView btnStartDiscussion;
    private TextView btnShareLink;

    private FirebaseFirestore db;
    private ListenerRegistration participantListener;

    private String sessionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host_waiting_room);

        tvSessionId = findViewById(R.id.tvSessionId);
        tvTopic = findViewById(R.id.tvTopic);
        tvParticipants = findViewById(R.id.tvParticipants);
        btnStartDiscussion = findViewById(R.id.btnStartDiscussion);
        btnShareLink = findViewById(R.id.btnShareLink);

        db = FirebaseFirestore.getInstance();

        sessionId = getIntent().getStringExtra("sessionId");

        if (sessionId == null || sessionId.isEmpty()) {
            Toast.makeText(this, "Invalid Session ID", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        tvSessionId.setText("Session ID: " + sessionId);

        loadDiscussionDetails();
        listenForParticipants();

        btnStartDiscussion.setOnClickListener(v -> startDiscussion());
        btnShareLink.setOnClickListener(v -> shareMeetingLink());
    }

    private void loadDiscussionDetails() {

        db.collection("discussions")
                .document(sessionId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (documentSnapshot.exists()) {

                        String topic = documentSnapshot.getString("topic");

                        if (topic != null) {
                            tvTopic.setText(topic);
                        }

                    }
                });
    }

    private void listenForParticipants() {

        participantListener = db.collection("discussions")
                .document(sessionId)
                .collection("participants")
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {
                        return;
                    }

                    if (snapshots != null) {

                        int count = snapshots.size();

                        tvParticipants.setText(
                                "👥  Participants joined: " + count
                        );
                    }
                });
    }

    private void startDiscussion() {

        db.collection("discussions")
                .document(sessionId)
                .update("status", "started")
                .addOnSuccessListener(aVoid -> {

                    Toast.makeText(
                            HostWaitingRoomActivity.this,
                            "Discussion started!",
                            Toast.LENGTH_SHORT
                    ).show();

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            HostWaitingRoomActivity.this,
                            "Failed to start discussion: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();

                });
    }
    private void shareMeetingLink() {

        String meetingLink = "https://ai-gd-analyzer.web.app/join/" + sessionId;

        android.content.Intent shareIntent = new android.content.Intent();
        shareIntent.setAction(android.content.Intent.ACTION_SEND);
        shareIntent.putExtra(
                android.content.Intent.EXTRA_TEXT,
                "Join my AI Group Discussion:\n\n" + meetingLink
        );
        shareIntent.setType("text/plain");

        startActivity(
                android.content.Intent.createChooser(
                        shareIntent,
                        "Share Meeting Link"
                )
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (participantListener != null) {
            participantListener.remove();
        }
    }
}