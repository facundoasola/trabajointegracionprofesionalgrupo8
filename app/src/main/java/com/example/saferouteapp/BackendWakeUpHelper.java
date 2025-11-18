package com.example.saferouteapp;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BackendWakeUpHelper {

    private static final String TAG = "BackendWakeUp";

    public interface WakeUpCallback {
        void onBackendReady();
        void onBackendTimeout();
    }

    public static void wakeUpBackend(Context context, WakeUpCallback callback) {
        Log.d(TAG, "🌅 Intentando despertar el backend...");

        Toast.makeText(context, "⏳ Despertando servidor, esto puede tardar hasta 1 minuto...",
                Toast.LENGTH_LONG).show();

        // Hacer una llamada simple para despertar el backend
        ApiClient.getService().getCrimenes().enqueue(new Callback<java.util.List<CrimeDto>>() {
            @Override
            public void onResponse(Call<java.util.List<CrimeDto>> call, Response<java.util.List<CrimeDto>> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "✅ Backend despierto y funcionando");
                    Toast.makeText(context, "✅ Servidor listo", Toast.LENGTH_SHORT).show();
                    callback.onBackendReady();
                } else {
                    Log.w(TAG, "⚠️ Backend responde pero con error: " + response.code());
                    // Aún así consideramos que está despierto
                    callback.onBackendReady();
                }
            }

            @Override
            public void onFailure(Call<java.util.List<CrimeDto>> call, Throwable t) {
                Log.e(TAG, "❌ Backend sigue durmiendo: " + t.getMessage());

                if (t instanceof java.net.SocketTimeoutException) {
                    Toast.makeText(context,
                            "❌ El servidor tardó demasiado en responder. Intenta de nuevo en unos minutos.",
                            Toast.LENGTH_LONG).show();
                    callback.onBackendTimeout();
                } else {
                    Toast.makeText(context,
                            "❌ Error de conexión: " + t.getMessage(),
                            Toast.LENGTH_LONG).show();
                    callback.onBackendTimeout();
                }
            }
        });
    }
}
