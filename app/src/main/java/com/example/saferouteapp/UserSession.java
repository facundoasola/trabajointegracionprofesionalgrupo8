package com.example.saferouteapp;

public class UserSession {

    private static UserResponse currentUser;

    public static void setCurrentUser(UserResponse user) {
        currentUser = user;
    }

    public static UserResponse getCurrentUser() {
        return currentUser;
    }

    public static String getCurrentUserMail() {
        return currentUser != null ? currentUser.mail : null;
    }

    public static int getCurrentUserPoints() {
        return currentUser != null ? currentUser.points : 0;
    }

    public static void updatePoints(int newPoints) {
        if (currentUser != null) {
            currentUser.points = newPoints;
        }
    }

    public static void clear() {
        currentUser = null;
    }
}
