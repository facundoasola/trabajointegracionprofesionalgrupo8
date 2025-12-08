package com.example.saferouteapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.stream.Collectors;

public class PointsActivity extends AppCompatActivity {

    private TextView pointsTextView, userNameTextView, userEmailTextView, logrostextView, verificacionestextView, confirmacionestextView;
    private Button backButton;
    private androidx.cardview.widget.CardView pointsCard, achievementsCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_points);

        // Inicializar vistas
        pointsTextView = findViewById(R.id.points_text_view);
        userNameTextView = findViewById(R.id.user_name_text_view);
        userEmailTextView = findViewById(R.id.user_email_text_view);
        backButton = findViewById(R.id.back_button);
        logrostextView = findViewById(R.id.logros_text_view);
        verificacionestextView = findViewById(R.id.verificaciones_text_view);
        confirmacionestextView = findViewById(R.id.confirmaciones_text_view);
        pointsCard = findViewById(R.id.points_card);
        achievementsCard = findViewById(R.id.achievements_card);

        // Mostrar datos del usuario actual
        displayUserInfo();

        // Botón para volver
        backButton.setOnClickListener(v -> finish());
        
        // Card de puntos clickeable - abre marketplace
        pointsCard.setOnClickListener(v -> {
            Intent intent = new Intent(PointsActivity.this, MarketplaceActivity.class);
            startActivity(intent);
        });
        
        // Card de logros clickeable - abre achievements
        achievementsCard.setOnClickListener(v -> {
            Intent intent = new Intent(PointsActivity.this, AchievementsActivity.class);
            startActivity(intent);
        });
    }

    private void displayUserInfo() {
        UserResponse user = UserSession.getCurrentUser();
        if (user != null) {
            userNameTextView.setText("👤 " + user.name + " " + user.surname);
            userEmailTextView.setText("📧 " + user.mail);
            pointsTextView.setText(String.valueOf(user.points));
            
            // Mostrar logros en formato "X / 6"
            int totalAchievements = user.achievements != null ? user.achievements.size() : 0;
            logrostextView.setText(totalAchievements + " / 6");
            
            verificacionestextView.setText(String.valueOf(user.validations));
            confirmacionestextView.setText(String.valueOf(user.confirmedReports));
        } else {
            Toast.makeText(this, "No hay usuario en sesión", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}

