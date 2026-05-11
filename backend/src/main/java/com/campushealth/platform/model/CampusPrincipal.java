package com.campushealth.platform.model;

public record CampusPrincipal(
        String userId,
        String displayName,
        UserRole role,
        String studentId
) {
    public boolean isStudent() {
        return role == UserRole.STUDENT;
    }

    public boolean isStaff() {
        return role == UserRole.STAFF;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
}