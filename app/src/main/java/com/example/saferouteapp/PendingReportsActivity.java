package com.example.saferouteapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PendingReportsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PendingReportsAdapter adapter;
    private List<CrimeDto> pendingReports = new ArrayList<>();
    private TextView emptyTextView;
    private Button backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_reports);

        recyclerView = findViewById(R.id.pending_reports_recycler_view);
        emptyTextView = findViewById(R.id.empty_text_view);
        backButton = findViewById(R.id.back_button);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PendingReportsAdapter(pendingReports);
        recyclerView.setAdapter(adapter);

        backButton.setOnClickListener(v -> finish());

        loadPendingReports();
    }

    private void loadPendingReports() {
        ApiClient.getService().getCrimenes().enqueue(new Callback<List<CrimeDto>>() {
            @Override
            public void onResponse(Call<List<CrimeDto>> call, Response<List<CrimeDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<CrimeDto> allCrimes = response.body();
                    pendingReports.clear();

                    // Filtrar solo los reportes no confirmados
                    for (CrimeDto crime : allCrimes) {
                        if (!crime.confirmed) {
                            pendingReports.add(crime);
                        }
                    }

                    adapter.notifyDataSetChanged();

                    if (pendingReports.isEmpty()) {
                        emptyTextView.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        emptyTextView.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                } else {
                    Toast.makeText(PendingReportsActivity.this,
                            "Error al cargar reportes",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<CrimeDto>> call, Throwable t) {
                Toast.makeText(PendingReportsActivity.this,
                        "Error de conexión: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class PendingReportsAdapter extends RecyclerView.Adapter<PendingReportsAdapter.ViewHolder> {
        private List<CrimeDto> reports;

        PendingReportsAdapter(List<CrimeDto> reports) {
            this.reports = reports;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_pending_report, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CrimeDto report = reports.get(position);

            holder.titleTextView.setText("🚨 " + report.type);
            holder.addressTextView.setText("📍 " + report.address);
            holder.descriptionTextView.setText(report.description);
            holder.verificationsTextView.setText("✓ Verificaciones: " + report.verifications);
            holder.reporterTextView.setText("👤 Reportado por: " + report.reporter);

            // Restaurar ambos botones: verificar y confirmar
            holder.verifyButton.setOnClickListener(v -> showVerifyDialog(report));
            holder.confirmButton.setOnClickListener(v -> showConfirmDialog(report));
        }

        @Override
        public int getItemCount() {
            return reports.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView titleTextView, addressTextView, descriptionTextView,
                    verificationsTextView, reporterTextView;
            Button verifyButton, confirmButton;

            ViewHolder(View itemView) {
                super(itemView);
                titleTextView = itemView.findViewById(R.id.title_text_view);
                addressTextView = itemView.findViewById(R.id.address_text_view);
                descriptionTextView = itemView.findViewById(R.id.description_text_view);
                verificationsTextView = itemView.findViewById(R.id.verifications_text_view);
                reporterTextView = itemView.findViewById(R.id.reporter_text_view);
                verifyButton = itemView.findViewById(R.id.verify_button);
                confirmButton = itemView.findViewById(R.id.confirm_button);
            }
        }
    }

    private void showVerifyDialog(CrimeDto report) {
        new AlertDialog.Builder(this)
                .setTitle("✓ Verificar Reporte")
                .setMessage("¿Confirmas que este reporte es válido?\n\n" +
                        "Tipo: " + report.type + "\n" +
                        "Ubicación: " + report.address + "\n\n" +
                        "Tu verificación ayudará a validar este incidente y podrías ganar logros.")
                .setPositiveButton("Sí, verificar", (dialog, which) -> verifyReport(report))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showConfirmDialog(CrimeDto report) {
        new AlertDialog.Builder(this)
                .setTitle("🎯 Confirmar Reporte")
                .setMessage("⚠️ ATENCIÓN: Esta acción confirmará oficialmente el reporte.\n\n" +
                        "Tipo: " + report.type + "\n" +
                        "Ubicación: " + report.address + "\n" +
                        "Verificaciones actuales: " + report.verifications + "\n\n" +
                        "Los logros se actualizan automáticamente.\n\n" +
                        "¿Estás seguro?")
                .setPositiveButton("Sí, confirmar", (dialog, which) -> confirmReport(report))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void verifyReport(CrimeDto report) {
        String userEmail = UserSession.getCurrentUserMail();
        if (userEmail == null) {
            Toast.makeText(this, "Error: Usuario no logueado", Toast.LENGTH_SHORT).show();
            return;
        }

        CrimeVerifyRequest request = new CrimeVerifyRequest(report.id, userEmail);

        ApiClient.getService().verificarCrimen(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(PendingReportsActivity.this,
                            "✅ Reporte verificado. El backend maneja los logros automáticamente.",
                            Toast.LENGTH_SHORT).show();
                    loadPendingReports(); // Recargar lista
                } else {
                    Toast.makeText(PendingReportsActivity.this,
                            "Error al verificar reporte (código: " + response.code() + ")",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(PendingReportsActivity.this,
                        "Error de conexión: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmReport(CrimeDto report) {
        CrimeIdRequest request = new CrimeIdRequest(report.id);

        ApiClient.getService().confirmarCrimen(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(PendingReportsActivity.this,
                            "🎉 Reporte confirmado! Los logros se actualizan automáticamente.",
                            Toast.LENGTH_LONG).show();
                    loadPendingReports(); // Recargar lista
                } else {
                    Toast.makeText(PendingReportsActivity.this,
                            "Error al confirmar reporte (código: " + response.code() + ")",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(PendingReportsActivity.this,
                        "Error de conexión: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}

