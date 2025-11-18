package com.example.saferouteapp;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NetworkUtils {

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }

    public static void testBackendConnection(Context context) {
        Log.d("NetworkUtils", "🔍 Probando conexión al backend...");

        if (!isNetworkAvailable(context)) {
            Toast.makeText(context, "❌ Sin conexión a internet", Toast.LENGTH_LONG).show();
            return;
        }

        // Hacer una simple llamada GET al endpoint de crímenes para probar
        ApiClient.getService().getCrimenes().enqueue(new Callback<java.util.List<CrimeDto>>() {
            @Override
            public void onResponse(Call<java.util.List<CrimeDto>> call, Response<java.util.List<CrimeDto>> response) {
                if (response.isSuccessful()) {
                    Log.d("NetworkUtils", "✅ Backend responde correctamente");
                    Toast.makeText(context, "✅ Conexión al backend exitosa", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("NetworkUtils", "❌ Backend respondió con código: " + response.code());
                    Toast.makeText(context, "⚠️ Backend devolvió error: " + response.code(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<java.util.List<CrimeDto>> call, Throwable t) {
                Log.e("NetworkUtils", "❌ Error de conexión: " + t.getMessage());
                Toast.makeText(context, "❌ Error de conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
