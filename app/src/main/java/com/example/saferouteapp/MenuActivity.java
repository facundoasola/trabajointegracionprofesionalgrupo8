package com.example.saferouteapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;

public class MenuActivity extends AppCompatActivity {

    private TextView userNameTextView, userPointsTextView;
    private Button viewPointsButton, myCrimesButton, logoutButton, closeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        // Inicializar vistas
        userNameTextView = findViewById(R.id.user_name_text_view);
        userPointsTextView = findViewById(R.id.user_points_text_view);
        viewPointsButton = findViewById(R.id.view_points_button);
        myCrimesButton = findViewById(R.id.my_crimes_button);
        logoutButton = findViewById(R.id.logout_button);
        closeButton = findViewById(R.id.close_button);

        // Mostrar información del usuario y cargar datos actualizados
        displayUserInfo();
        refreshUserData();

        // Configurar botones
        viewPointsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, PointsActivity.class);
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
        refreshUserData();
    }

    private void displayUserInfo() {
        UserResponse user = UserSession.getCurrentUser();
        if (user != null) {
            userNameTextView.setText("👤 " + user.name + " " + user.surname);
            userPointsTextView.setText("🏆 " + user.points + " puntos");
        }
    }

    private void refreshUserData() {
        String userMail = UserSession.getCurrentUserMail();
        if (userMail == null) {
            Log.w("MenuActivity", "No hay usuario logueado");
            return;
        }

        Log.d("MenuActivity", "📡 Actualizando datos del usuario: " + userMail);

        UserMailRequest request = new UserMailRequest(userMail);
        
        ApiClient.getService().getUsuario(request).enqueue(new Callback<List<UserResponse>>() {
            @Override
            public void onResponse(Call<List<UserResponse>> call, Response<List<UserResponse>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    UserResponse updatedUser = response.body().get(0);
                    
                    Log.d("MenuActivity", "✅ Datos del usuario actualizados. Puntos: " + updatedUser.points);
                    
                    // Actualizar la sesión con los datos frescos
                    UserSession.setCurrentUser(updatedUser);
                    
                    // Actualizar la interfaz
                    displayUserInfo();
                    Toast.makeText(MenuActivity.this, "✅ Perfil actualizado", Toast.LENGTH_SHORT).show();
                    
                } else {
                    Log.e("MenuActivity", "❌ Error al obtener datos del usuario: " + response.code());
                    Toast.makeText(MenuActivity.this, "⚠️ No se pudieron actualizar los datos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<UserResponse>> call, Throwable t) {
                Log.e("MenuActivity", "💥 Error de conexión al actualizar usuario: " + t.getMessage());
                if (t instanceof java.net.SocketTimeoutException) {
                    Toast.makeText(MenuActivity.this, 
                        "⏳ El servidor tardó en responder. Los datos se actualizarán pronto.", 
                        Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MenuActivity.this, 
                        "❌ Error de conexión: " + t.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                }
            }
        });
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

