package com.campushealth.platform.dto;

import com.campushealth.platform.model.StudentProfile;

public record StudentRegistrationResponse(
        String token,
        StudentProfile student
) {
}