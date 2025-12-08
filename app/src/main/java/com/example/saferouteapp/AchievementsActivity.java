package com.example.saferouteapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import java.util.ArrayList;
import java.util.List;

public class AchievementsActivity extends AppCompatActivity {

    private TextView totalAchievementsText;
    private LinearLayout achievementsContainer;
    private Button backButton;

    // Logros disponibles según el backend (db.go)
    private static class Achievement {
        String category;
        String name;
        int requirements;
        String displayName;
        String description;
        String emoji;

        Achievement(String category, String name, int requirements, String displayName, String description, String emoji) {
            this.category = category;
            this.name = name;
            this.requirements = requirements;
            this.displayName = displayName;
            this.description = description;
            this.emoji = emoji;
        }
    }

    private final List<Achievement> allAchievements = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievements);

        // Inicializar vistas
        totalAchievementsText = findViewById(R.id.total_achievements_text);
        achievementsContainer = findViewById(R.id.achievements_container);
        backButton = findViewById(R.id.achievements_back_button);

        // Definir todos los logros disponibles (según db.go)
        initializeAllAchievements();

        // Mostrar logros
        displayAchievements();

        // Botón de volver
        backButton.setOnClickListener(v -> finish());
    }

    private void initializeAllAchievements() {
        // Logros de CONFIRMACIÓN (reportes confirmados)
        allAchievements.add(new Achievement(
            "CONFIRMATION", "REPORTERO", 3,
            "🎯 Reportero",
            "Obtén 3 reportes confirmados",
            "🎯"
        ));
        allAchievements.add(new Achievement(
            "CONFIRMATION", "LUZ_NOCTURNA", 10,
            "🌟 Luz Nocturna",
            "Obtén 10 reportes confirmados",
            "🌟"
        ));
        allAchievements.add(new Achievement(
            "CONFIRMATION", "OJO_DE_SAURON", 20,
            "👁️ Ojo de Sauron",
            "Obtén 20 reportes confirmados",
            "👁️"
        ));

        // Logros de VALIDACIÓN (verificaciones realizadas)
        allAchievements.add(new Achievement(
            "VALIDATION", "DETECTIVE", 15,
            "🔍 Detective",
            "Realiza 15 verificaciones",
            "🔍"
        ));
        allAchievements.add(new Achievement(
            "VALIDATION", "ROBIN", 50,
            "🦸 Robin",
            "Realiza 50 verificaciones",
            "🦸"
        ));
        allAchievements.add(new Achievement(
            "VALIDATION", "BATMAN", 100,
            "🦇 Batman",
            "Realiza 100 verificaciones",
            "🦇"
        ));
    }

    private void displayAchievements() {
        UserResponse user = UserSession.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "No hay usuario en sesión", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Contar logros obtenidos
        int unlockedCount = user.achievements != null ? user.achievements.size() : 0;
        totalAchievementsText.setText(unlockedCount + " / " + allAchievements.size() + " Logros Desbloqueados");

        // Crear lista de nombres de logros obtenidos para comparación
        List<String> unlockedNames = new ArrayList<>();
        if (user.achievements != null) {
            for (Logro logro : user.achievements) {
                unlockedNames.add(logro.name);
            }
        }

        // Mostrar todos los logros
        for (Achievement achievement : allAchievements) {
            boolean isUnlocked = unlockedNames.contains(achievement.name);
            addAchievementCard(achievement, isUnlocked, user);
        }
    }

    private void addAchievementCard(Achievement achievement, boolean isUnlocked, UserResponse user) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_achievement, achievementsContainer, false);

        ImageView lockIcon = cardView.findViewById(R.id.achievement_lock_icon);
        TextView emojiText = cardView.findViewById(R.id.achievement_emoji);
        TextView nameText = cardView.findViewById(R.id.achievement_name);
        TextView descriptionText = cardView.findViewById(R.id.achievement_description);
        TextView progressText = cardView.findViewById(R.id.achievement_progress);
        CardView card = cardView.findViewById(R.id.achievement_card);

        // Configurar apariencia según si está desbloqueado
        if (isUnlocked) {
            lockIcon.setVisibility(View.GONE);
            emojiText.setVisibility(View.VISIBLE);
            emojiText.setText(achievement.emoji);
            nameText.setText(achievement.displayName);
            descriptionText.setText("✅ " + achievement.description);
            progressText.setVisibility(View.GONE);
            card.setCardBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
        } else {
            lockIcon.setVisibility(View.VISIBLE);
            emojiText.setVisibility(View.GONE);
            nameText.setText("🔒 " + achievement.displayName);
            descriptionText.setText(achievement.description);
            
            // Mostrar progreso
            int currentProgress = 0;
            if (achievement.category.equals("CONFIRMATION")) {
                currentProgress = user.confirmedReports;
            } else if (achievement.category.equals("VALIDATION")) {
                currentProgress = user.validations;
            }
            progressText.setText("Progreso: " + currentProgress + " / " + achievement.requirements);
            progressText.setVisibility(View.VISIBLE);
            card.setCardBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        }

        achievementsContainer.addView(cardView);
    }
}
