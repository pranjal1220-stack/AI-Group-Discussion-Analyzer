package com.example.gdanalyzer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class DashboardActivity extends AppCompatActivity {

    private TextView btnStartGD;
    private TextView btnJoinGD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnStartGD = findViewById(R.id.btnStartGD);
        btnJoinGD = findViewById(R.id.btnJoinGD);

        btnStartGD.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, CreateGDActivity.class);
            startActivity(intent);
        });

        btnJoinGD.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, JoinGDActivity.class);
            startActivity(intent);
        });
    }
}