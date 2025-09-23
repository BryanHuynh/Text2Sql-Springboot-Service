package com.text2sql.text2sql_springboot.security;

public class AppContext {
    private static final ThreadLocal<String> currentUserId = new ThreadLocal<>();

    private AppContext() {
    }

    public static void setCurrentUserId(String userId) {
        currentUserId.set(userId);
    }

    public static String getCurrentUserId() {
        return currentUserId.get();
    }

    public static void clear() {
        currentUserId.remove();
    }
}
