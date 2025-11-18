package com.example.saferouteapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyCrimesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MyCrimesAdapter adapter;
    private List<CrimeDto> myCrimes = new ArrayList<>();
    private TextView emptyTextView;
    private Button backButton, refreshButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_crimes);

        recyclerView = findViewById(R.id.my_crimes_recycler_view);
        emptyTextView = findViewById(R.id.empty_text_view);
        backButton = findViewById(R.id.back_button);
        refreshButton = findViewById(R.id.refresh_button);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyCrimesAdapter(myCrimes);
        recyclerView.setAdapter(adapter);

        backButton.setOnClickListener(v -> finish());
        refreshButton.setOnClickListener(v -> loadMyCrimes());

        loadMyCrimes();
    }

    private void loadMyCrimes() {
        String userEmail = UserSession.getCurrentUserMail();
        if (userEmail == null) {
            Toast.makeText(this, "Error: No hay usuario en sesión", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ApiClient.getService().getCrimenes().enqueue(new Callback<List<CrimeDto>>() {
            @Override
            public void onResponse(Call<List<CrimeDto>> call, Response<List<CrimeDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<CrimeDto> allCrimes = response.body();
                    myCrimes.clear();

                    // Filtrar solo los crímenes reportados por el usuario actual
                    for (CrimeDto crime : allCrimes) {
                        if (crime.reporter != null && crime.reporter.equals(userEmail)) {
                            myCrimes.add(crime);
                        }
                    }

                    adapter.notifyDataSetChanged();

                    if (myCrimes.isEmpty()) {
                        emptyTextView.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        emptyTextView.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }

                    // Si hay crímenes confirmados, actualizar puntos del usuario
                    boolean hasConfirmedCrimes = false;
                    for (CrimeDto crime : myCrimes) {
                        if (crime.confirmed) {
                            hasConfirmedCrimes = true;
                            break;
                        }
                    }

                    if (hasConfirmedCrimes) {
                        updateUserPoints();
                    }
                } else {
                    Toast.makeText(MyCrimesActivity.this,
                            "Error al cargar reportes (código: " + response.code() + ")",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<CrimeDto>> call, Throwable t) {
                Toast.makeText(MyCrimesActivity.this,
                        "Error de conexión: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUserPoints() {
        String userEmail = UserSession.getCurrentUserMail();
        if (userEmail == null) return;

        UserMailRequest request = new UserMailRequest(userEmail);

        ApiClient.getService().getUsuario(request).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserResponse updatedUser = response.body();
                    UserSession.setCurrentUser(updatedUser);
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                // Silently fail - no interrumpir la experiencia del usuario
            }
        });
    }

    private class MyCrimesAdapter extends RecyclerView.Adapter<MyCrimesAdapter.ViewHolder> {
        private List<CrimeDto> crimes;

        MyCrimesAdapter(List<CrimeDto> crimes) {
            this.crimes = crimes;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_my_crime, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CrimeDto crime = crimes.get(position);

            holder.titleTextView.setText("🚨 " + crime.type);
            holder.addressTextView.setText("📍 " + crime.address);
            holder.descriptionTextView.setText(crime.description);
            holder.verificationsTextView.setText("✓ Verificaciones: " + crime.verifications);

            if (crime.confirmed) {
                holder.statusTextView.setText("✅ CONFIRMADO");
                holder.statusTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                holder.earnedPointsTextView.setVisibility(View.VISIBLE);
                holder.earnedPointsTextView.setText("🏆 +10 puntos ganados");
            } else {
                holder.statusTextView.setText("⏳ PENDIENTE");
                holder.statusTextView.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                holder.earnedPointsTextView.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return crimes.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView titleTextView, addressTextView, descriptionTextView,
                    verificationsTextView, statusTextView, earnedPointsTextView;

            ViewHolder(View itemView) {
                super(itemView);
                titleTextView = itemView.findViewById(R.id.title_text_view);
                addressTextView = itemView.findViewById(R.id.address_text_view);
                descriptionTextView = itemView.findViewById(R.id.description_text_view);
                verificationsTextView = itemView.findViewById(R.id.verifications_text_view);
                statusTextView = itemView.findViewById(R.id.status_text_view);
                earnedPointsTextView = itemView.findViewById(R.id.earned_points_text_view);
            }
        }
    }
}

