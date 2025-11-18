package com.example.saferouteapp;

public class CrimeVerifyRequest {
    public long id;
    public String mail;

    public CrimeVerifyRequest(long id, String mail) {
        this.id = id;
        this.mail = mail;
    }
}
