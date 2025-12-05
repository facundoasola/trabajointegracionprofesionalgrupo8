package com.example.saferouteapp;

public class Category {
    private String code;

    private int value;

    public Category(String code, int value) {
        this.code = code;
        this.value = value;
    }

    public String getCode() {
        return code;
    }

    public int getValue() {
        return value;
    }
}
