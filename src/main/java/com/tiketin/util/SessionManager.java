package com.tiketin.util;

import com.tiketin.model.User;
import java.util.ArrayList;
import java.util.List;

public class SessionManager {
    private static User currentUser;
    private static final List<Runnable> listeners = new ArrayList<>();

    public static synchronized User getCurrentUser() {
        return currentUser;
    }

    public static synchronized void login(User user) {
        currentUser = user;
        notifyListeners();
    }

    public static synchronized void logout() {
        currentUser = null;
        notifyListeners();
    }

    public static synchronized boolean isLoggedIn() {
        return currentUser != null;
    }

    public static synchronized boolean hasRole(String role) {
        if (currentUser == null || currentUser.getRole() == null || role == null) {
            return false;
        }
        return currentUser.getRole().equalsIgnoreCase(role.trim());
    }

    public static synchronized void addChangeListener(Runnable listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public static void notifyListeners() {
        List<Runnable> copy;
        synchronized (SessionManager.class) {
            copy = new ArrayList<>(listeners);
        }
        for (Runnable listener : copy) {
            try {
                listener.run();
            } catch (Exception e) {
                System.err.println("Error pada session listener: " + e.getMessage());
            }
        }
    }
}
