package com.example.saferouteapp;

public class Category {
    private String code;
    private String type;
    private int value;

    public Category(String code, String type, int value) {
        this.code = code;
        this.type = type;
        this.value = value;
    }

    public String getCode() {
        return code;
    }

    public String getType() {
        return type;
    }

    public int getValue() {
        return value;
    }
}
