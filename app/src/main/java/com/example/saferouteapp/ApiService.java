package com.example.saferouteapp;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {

    // LOGIN: devuelve los datos del usuario + puntos
    @POST("login")
    Call<UserResponse> login(@Body LoginRequest request);

    // REGISTER: solo interesa si fue bien o no, el back no devuelve JSON
    @POST("register")
    Call<Void> register(@Body RegisterRequest request);

    // OBTENER USUARIO POR MAIL (para refrescar puntos, etc.)
    @POST("usuario")
    Call<UserResponse> getUsuario(@Body UserMailRequest request);

    // Más adelante agregamos acá los endpoints de crímenes:
    // @GET("crimenes")
    // @POST("crimen-nuevo")
    // @POST("verificacion-crimen")
    // @POST("confirmacion-crimen")
}
