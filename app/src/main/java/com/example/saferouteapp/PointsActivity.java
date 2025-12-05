package com.example.saferouteapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PointsActivity extends AppCompatActivity {

    private TextView pointsTextView, userNameTextView, userEmailTextView, logrostextView,verificacionestextView, confirmacionestextView;
    private Button refreshButton, backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_points);

        // Inicializar vistas
        pointsTextView = findViewById(R.id.points_text_view);
        userNameTextView = findViewById(R.id.user_name_text_view);
        userEmailTextView = findViewById(R.id.user_email_text_view);
        refreshButton = findViewById(R.id.refresh_button);
        backButton = findViewById(R.id.back_button);
        logrostextView = findViewById(R.id.logros_text_view);
        verificacionestextView = findViewById(R.id.verificaciones_text_view);
        confirmacionestextView = findViewById(R.id.confirmaciones_text_view);



        // Mostrar datos del usuario actual
        displayUserInfo();

        // Botón para refrescar puntos
        refreshButton.setOnClickListener(v -> refreshUserPoints());

        // Botón para volver
        backButton.setOnClickListener(v -> finish());
    }

    private void displayUserInfo() {
        UserResponse user = UserSession.getCurrentUser();
        if (user != null) {
            userNameTextView.setText("👤 " + user.name + " " + user.surname);
            userEmailTextView.setText("📧 " + user.mail);
            pointsTextView.setText(String.valueOf(user.points));
            logrostextView.setText(user.achievements.stream().map(Logro::getName).collect(Collectors.joining(", ")));
            verificacionestextView.setText(String.valueOf(user.validations));
            confirmacionestextView.setText(String.valueOf(user.confirmedReports));



        } else {
            Toast.makeText(this, "No hay usuario en sesión", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void refreshUserPoints() {
        String userEmail = UserSession.getCurrentUserMail();
        if (userEmail == null) {
            Toast.makeText(this, "Error: No hay usuario en sesión", Toast.LENGTH_SHORT).show();
            return;
        }

        UserMailRequest request = new UserMailRequest(userEmail);

        ApiClient.getService().getUsuario(request).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserResponse updatedUser = response.body();

                    // Actualizar sesión
                    UserSession.setCurrentUser(updatedUser);

                    // Actualizar UI
                    displayUserInfo();

                    Toast.makeText(PointsActivity.this,
                            "✅ Puntos actualizados",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(PointsActivity.this,
                            "Error al actualizar puntos",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                Toast.makeText(PointsActivity.this,
                        "Error de conexión: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}

