package com.studentportal.model;

public enum UserRole {
    STUDENT,
    FACULTY,
    ADMIN;

    public static UserRole fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Role is required");
        }
        return UserRole.valueOf(value.trim().toUpperCase());
    }
}
