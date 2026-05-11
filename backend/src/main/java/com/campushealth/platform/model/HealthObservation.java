package com.campushealth.platform.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record HealthObservation(
        @Min(0) @Max(24) double sleepHours,
        @Min(0) @Max(7) int lateNightCountPerWeek,
        @Min(0) @Max(100) int nutritionScore,
        @Min(0) @Max(100) int stressScore,
        @Min(0) int physicalActivityMinutesPerWeek,
        @Min(0) int infectionContacts,
        boolean feverReported,
        boolean coughReported
) {
}
