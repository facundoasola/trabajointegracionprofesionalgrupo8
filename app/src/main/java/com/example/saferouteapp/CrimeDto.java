package com.example.saferouteapp;

public class CrimeDto {
    public long id;
    public Category category;

    public String description;
    public String address;
    public String latitude;
    public String longitude;
    public String reporter;   // mail del usuario
    public int verification = 0; // cantidad de verificaciones
    public boolean confirmed = false; // si está confirmado

    public String status;

    public String time;

    // Constructor vacío para Retrofit
    public CrimeDto() {}

    // Constructor completo
    public CrimeDto(long id, Category category, String description, String address,
                    String latitude, String longitude, String reporter,
                    int verification, boolean confirmed, String time,String status) {
        this.id = id;
        this.category = category;
        this.description = description;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.reporter = reporter;
        this.verification = verification;
        this.confirmed = confirmed;
        this.time = time;
        this.status = status;

    }
}
