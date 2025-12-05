package com.example.saferouteapp;

public class CrimeCreateRequest {
    public String category;
    public String description;
    public String address;
    public String latitude;
    public String longitude;
    public String reporter;

    public String time;
    public CrimeCreateRequest(String category, String description, String address,
                              String latitude, String longitude, String reporter, String time) {
        this.category = category;
        this.description = description;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.reporter = reporter;
        this.time = time;
    }
}
