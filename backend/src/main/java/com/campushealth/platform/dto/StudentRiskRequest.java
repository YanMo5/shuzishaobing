package com.campushealth.platform.dto;

import com.campushealth.platform.model.HealthObservation;
import com.campushealth.platform.model.StudentProfile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record StudentRiskRequest(
        @NotNull @Valid StudentProfile student,
        @NotNull @Valid HealthObservation observation,
        String focus
) {
    public StudentRiskRequest {
        if (focus != null && focus.isBlank()) {
            focus = null;
        }
    }
}
