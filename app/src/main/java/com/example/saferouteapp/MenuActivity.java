package com.example.saferouteapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MenuActivity extends AppCompatActivity {

    private TextView userNameTextView, userPointsTextView;
    private Button viewPointsButton, pendingReportsButton, myCrimesButton, logoutButton, closeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        // Inicializar vistas
        userNameTextView = findViewById(R.id.user_name_text_view);
        userPointsTextView = findViewById(R.id.user_points_text_view);
        viewPointsButton = findViewById(R.id.view_points_button);
        pendingReportsButton = findViewById(R.id.pending_reports_button);
        myCrimesButton = findViewById(R.id.my_crimes_button);
        logoutButton = findViewById(R.id.logout_button);
        closeButton = findViewById(R.id.close_button);

        // Mostrar información del usuario
        displayUserInfo();

        // Configurar botones
        viewPointsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, PointsActivity.class);
            startActivity(intent);
        });

        pendingReportsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, PendingReportsActivity.class);
            startActivity(intent);
        });

        myCrimesButton.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, MyCrimesActivity.class);
            startActivity(intent);
        });

        logoutButton.setOnClickListener(v -> showLogoutDialog());

        closeButton.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Actualizar puntos cada vez que volvemos al menú
        displayUserInfo();
    }

    private void displayUserInfo() {
        UserResponse user = UserSession.getCurrentUser();
        if (user != null) {
            userNameTextView.setText("👤 " + user.name + " " + user.surname);
            userPointsTextView.setText("🏆 " + user.points + " puntos");
        }
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Cerrar Sesión")
                .setMessage("¿Estás seguro que deseas cerrar sesión?")
                .setPositiveButton("Sí", (dialog, which) -> logout())
                .setNegativeButton("No", null)
                .show();
    }

    private void logout() {
        UserSession.clear();
        Intent intent = new Intent(MenuActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

