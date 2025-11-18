package com.example.saferouteapp;

public class CrimeDto {
    public long id;
    public String type;
    public String description;
    public String address;
    public double latitude;
    public double longitude;
    public String reporter;   // mail del usuario
    public int verifications = 0; // cantidad de verificaciones
    public boolean confirmed = false; // si está confirmado

    // Constructor vacío para Retrofit
    public CrimeDto() {}

    // Constructor completo
    public CrimeDto(long id, String type, String description, String address,
                    double latitude, double longitude, String reporter,
                    int verifications, boolean confirmed) {
        this.id = id;
        this.type = type;
        this.description = description;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.reporter = reporter;
        this.verifications = verifications;
        this.confirmed = confirmed;
    }
}
