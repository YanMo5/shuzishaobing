package com.campushealth.platform.dto;

import com.campushealth.platform.model.HealthObservation;
import com.campushealth.platform.model.RiskAssessment;
import com.campushealth.platform.model.StudentProfile;

public record StudentSummaryResponse(
        StudentProfile student,
        HealthObservation observation,
        RiskAssessment assessment
) {
}
