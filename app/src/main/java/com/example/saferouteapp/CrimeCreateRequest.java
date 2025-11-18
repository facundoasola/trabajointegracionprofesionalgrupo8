package com.example.saferouteapp;

public class CrimeCreateRequest {
    public String type;
    public String description;
    public String address;
    public double latitude;
    public double longitude;
    public String reporter;

    public CrimeCreateRequest(String type, String description, String address,
                              double latitude, double longitude, String reporter) {
        this.type = type;
        this.description = description;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.reporter = reporter;
    }
}
