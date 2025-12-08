package com.example.saferouteapp;

public class Logro {
    public String category;     // "CONFIRMATION" o "VALIDATION"
    public String name;          // "REPORTERO", "DETECTIVE", etc.
    public int requirements;     // Número requerido para desbloquear

    public Logro(String category, String name, int requirements) {
        this.category = category;
        this.name = name;
        this.requirements = requirements;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public int getRequirements() {
        return requirements;
    }
}
