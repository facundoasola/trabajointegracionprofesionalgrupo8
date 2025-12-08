package com.example.saferouteapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;

public class MarketplaceActivity extends AppCompatActivity {

    private TextView userPointsText;
    private LinearLayout rewardsContainer;
    private Button backButton;
    private int userPoints = 0;

    // Recompensas disponibles
    private static class Reward {
        String title;
        String description;
        int cost;
        String brand;
        String emoji;

        Reward(String title, String description, int cost, String brand, String emoji) {
            this.title = title;
            this.description = description;
            this.cost = cost;
            this.brand = brand;
            this.emoji = emoji;
        }
    }

    private final Reward[] rewards = {
        new Reward("2x1 en Combo", "Hamburguesa + Papas + Bebida", 50, "McDonald's", "🍔"),
        new Reward("30% OFF", "En toda la tienda", 75, "Zara", "👕"),
        new Reward("10% OFF", "En combustible", 40, "YPF", "⛽"),
        new Reward("Café Gratis", "Cualquier tamaño", 25, "Starbucks", "☕"),
        new Reward("15% OFF", "En compras superiores a $5000", 60, "Falabella", "🛍️"),
        new Reward("Pizza Mediana", "Con 2 gustos a elección", 80, "Pizza Hut", "🍕")
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_marketplace);

        // Inicializar vistas
        userPointsText = findViewById(R.id.marketplace_points_text);
        rewardsContainer = findViewById(R.id.rewards_container);
        backButton = findViewById(R.id.marketplace_back_button);

        // Cargar puntos del usuario
        loadUserPoints();

        // Mostrar recompensas
        displayRewards();

        // Botón de volver
        backButton.setOnClickListener(v -> finish());
    }

    private void loadUserPoints() {
        UserResponse user = UserSession.getCurrentUser();
        if (user != null) {
            userPoints = user.points;
            updatePointsDisplay();
        } else {
            Toast.makeText(this, "No hay usuario en sesión", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void updatePointsDisplay() {
        userPointsText.setText("⭐ " + userPoints + " puntos disponibles");
    }

    private void displayRewards() {
        for (Reward reward : rewards) {
            addRewardCard(reward);
        }
    }

    private void addRewardCard(Reward reward) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_reward, rewardsContainer, false);

        TextView emojiText = cardView.findViewById(R.id.reward_emoji);
        TextView titleText = cardView.findViewById(R.id.reward_title);
        TextView descriptionText = cardView.findViewById(R.id.reward_description);
        TextView brandText = cardView.findViewById(R.id.reward_brand);
        TextView costText = cardView.findViewById(R.id.reward_cost);
        Button redeemButton = cardView.findViewById(R.id.reward_redeem_button);
        CardView card = cardView.findViewById(R.id.reward_card);

        emojiText.setText(reward.emoji);
        titleText.setText(reward.title);
        descriptionText.setText(reward.description);
        brandText.setText("📍 " + reward.brand);
        costText.setText(reward.cost + " pts");

        // Verificar si tiene suficientes puntos
        if (userPoints >= reward.cost) {
            redeemButton.setEnabled(true);
            redeemButton.setText("Canjear");
            card.setCardBackgroundColor(getResources().getColor(android.R.color.white));
        } else {
            redeemButton.setEnabled(false);
            redeemButton.setText("Insuficiente");
            card.setCardBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        }

        redeemButton.setOnClickListener(v -> showRedeemDialog(reward));

        rewardsContainer.addView(cardView);
    }

    private void showRedeemDialog(Reward reward) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🎁 Confirmar Canje");
        builder.setMessage(
            reward.emoji + " " + reward.title + "\n" +
            "📍 " + reward.brand + "\n" +
            "💰 Costo: " + reward.cost + " puntos\n\n" +
            "Puntos actuales: " + userPoints + "\n" +
            "Puntos después: " + (userPoints - reward.cost)
        );

        builder.setPositiveButton("Canjear", (dialog, which) -> {
            redeemReward(reward);
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void redeemReward(Reward reward) {
        // En una app real, aquí se haría una llamada al backend para registrar el canje
        // Por ahora, simulamos el canje localmente
        
        // Descontar puntos localmente
        userPoints -= reward.cost;
        
        // Generar código único
        String code = reward.brand.toUpperCase().replaceAll(" ", "") + "-" + 
                     (System.currentTimeMillis() % 100000);

        // Actualizar el usuario en el backend (simulado)
        updateUserPointsInBackend();

        // Mostrar código de descuento
        showRedeemSuccessDialog(reward, code);
    }

    private void updateUserPointsInBackend() {
        // Aquí deberíamos actualizar los puntos en el backend
        // Por ahora solo actualizamos la sesión local
        UserResponse user = UserSession.getCurrentUser();
        if (user != null) {
            user.points = userPoints;
            UserSession.setCurrentUser(user);
        }
        
        updatePointsDisplay();
        
        // Recargar las tarjetas de recompensas
        rewardsContainer.removeAllViews();
        displayRewards();
    }

    private void showRedeemSuccessDialog(Reward reward, String code) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("✅ ¡Canje Exitoso!");
        builder.setMessage(
            "🎉 ¡Felicitaciones!\n\n" +
            "Tu código de descuento:\n\n" +
            "📱 " + code + "\n\n" +
            reward.emoji + " " + reward.title + "\n" +
            "📍 " + reward.brand + "\n\n" +
            "Presenta este código en la tienda\n" +
            "⏰ Válido por 30 días"
        );

        builder.setPositiveButton("Aceptar", (dialog, which) -> {
            Toast.makeText(this, "¡Disfruta tu recompensa! 🎉", Toast.LENGTH_LONG).show();
        });

        builder.setCancelable(false);
        builder.show();
    }
}
