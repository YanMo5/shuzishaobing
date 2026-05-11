package com.campushealth.platform.dto;

import com.campushealth.platform.model.HealthDataSourceType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CampusHealthSignalRequest(
        @NotNull HealthDataSourceType sourceType,
        @Min(0) @Max(24) double sleepHours,
        @Min(0) int lateNightCountPerWeek,
        @Min(0) @Max(100) int nutritionScore,
        @Min(0) @Max(100) int stressScore,
        @Min(0) int physicalActivityMinutesPerWeek,
        @Min(0) int infectionContacts,
        boolean feverReported,
        boolean coughReported,
        String note
) {
}