package com.campushealth.platform.dto;

public record LoginResponse(
        String token,
        String userId,
        String name,
        String role
) {
}