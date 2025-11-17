package com.example.saferouteapp;

public class RegisterRequest {
    public String mail;
    public String password;
    public String name;
    public String surname;

    public RegisterRequest(String mail, String password, String name, String surname) {
        this.mail = mail;
        this.password = password;
        this.name = name;
        this.surname = surname;
    }
}
