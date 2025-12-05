package com.example.saferouteapp;

import java.util.List;

public class UserResponse {
    public String mail;
    public String name;
    public String surname;
    public int points;   // ajustar si en el back se llama distinto
    public int confirmedReports;
    public int validations;
    public List<Logro> achievements;
    // Podés agregar más campos si el back los manda (id, etc.)
}
