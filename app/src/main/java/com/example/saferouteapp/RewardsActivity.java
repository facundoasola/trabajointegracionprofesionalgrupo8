package com.example.saferouteapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class RewardsActivity extends AppCompatActivity {
    
    private TextView pointsTextView;
    private int userPoints;
    private static final String PREFS_NAME = "SafeRoutePrefs";
    private static final String KEY_POINTS = "user_points";
    private static final int REWARD_COST = 500;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rewards);
        
        // Cargar puntos del usuario
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        userPoints = prefs.getInt(KEY_POINTS, 0);
        
        // Referencias UI
        pointsTextView = findViewById(R.id.rewards_points_text);
        updatePointsDisplay();
        
        // Botón de volver
        Button backButton = findViewById(R.id.rewards_back_button);
        backButton.setOnClickListener(v -> finish());
        
        // Configurar los 3 descuentos
        setupReward(R.id.burger_king_card, R.id.burger_king_redeem, 
                   "Burger King", "15% de descuento", "burger_king");
        
        setupReward(R.id.zara_card, R.id.zara_redeem, 
                   "Zara", "15% de descuento", "zara");
        
        setupReward(R.id.ypf_card, R.id.ypf_redeem, 
                   "YPF", "10% de descuento", "ypf");
    }
    
    private void setupReward(int cardId, int buttonId, String brandName, String discount, String rewardCode) {
        Button redeemButton = findViewById(buttonId);
        redeemButton.setOnClickListener(v -> showRedeemDialog(brandName, discount, rewardCode));
    }
    
    private void showRedeemDialog(String brandName, String discount, String rewardCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🎁 Canjear Descuento");
        
        if (userPoints >= REWARD_COST) {
            builder.setMessage(
                "¿Deseas canjear " + REWARD_COST + " puntos por:\n\n" +
                "🏪 " + brandName + "\n" +
                "💰 " + discount + "\n\n" +
                "Puntos actuales: " + userPoints + "\n" +
                "Puntos después del canje: " + (userPoints - REWARD_COST)
            );
            
            builder.setPositiveButton("Canjear", (dialog, which) -> {
                redeemReward(brandName, discount, rewardCode);
            });
            
            builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());
        } else {
            builder.setMessage(
                "❌ Puntos insuficientes\n\n" +
                "Necesitas: " + REWARD_COST + " puntos\n" +
                "Tienes: " + userPoints + " puntos\n" +
                "Te faltan: " + (REWARD_COST - userPoints) + " puntos\n\n" +
                "💡 Sigue reportando y apoyando incidentes\n" +
                "para ganar más puntos!"
            );
            
            builder.setPositiveButton("Entendido", (dialog, which) -> dialog.dismiss());
        }
        
        builder.show();
    }
    
    private void redeemReward(String brandName, String discount, String rewardCode) {
        // Descontar puntos
        userPoints -= REWARD_COST;
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putInt(KEY_POINTS, userPoints).apply();
        updatePointsDisplay();
        
        // Generar código de descuento único
        String uniqueCode = rewardCode.toUpperCase() + "-" + System.currentTimeMillis() % 100000;
        
        // Mostrar código de descuento
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("✅ ¡Descuento Canjeado!");
        builder.setMessage(
            "🎉 ¡Felicitaciones!\n\n" +
            "Has canjeado tu descuento de:\n" +
            "🏪 " + brandName + "\n" +
            "💰 " + discount + "\n\n" +
            "📱 Tu código de descuento:\n" +
            "🔑 " + uniqueCode + "\n\n" +
            "Presenta este código en cualquier sucursal\n" +
            "o tienda online de " + brandName + ".\n\n" +
            "⭐ Puntos restantes: " + userPoints
        );
        builder.setPositiveButton("Guardar", (dialog, which) -> {
            Toast.makeText(this, "Código guardado: " + uniqueCode, Toast.LENGTH_LONG).show();
            dialog.dismiss();
        });
        builder.setCancelable(false);
        builder.show();
    }
    
    private void updatePointsDisplay() {
        pointsTextView.setText("⭐ Tienes " + userPoints + " puntos");
    }
}
