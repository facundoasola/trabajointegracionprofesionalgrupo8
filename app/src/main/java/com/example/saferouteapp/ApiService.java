package com.example.saferouteapp;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {

    @POST("api/login")
    Call<UserResponse> login(@Body LoginRequest request);

    @POST("api/register")
    Call<UserResponse> register(@Body RegisterRequest request);

    @POST("api/usuarios")
    Call<UserResponse> getUsuario(@Body UserMailRequest request);

    @GET("api/crimenes")
    Call<List<CrimeDto>> getCrimenes();

    @POST("api/crimen-nuevo")
    Call<CrimeDto> crearCrimen(@Body CrimeCreateRequest request);

    @POST("api/verificacion-crimen")
    Call<Void> verificarCrimen(@Body CrimeVerifyRequest request);

    @POST("api/confirmacion-crimen")
    Call<Void> confirmarCrimen(@Body CrimeIdRequest request);
}
