package com.campushealth.platform.model;

import java.time.Instant;

public record CampusHealthSignal(
        String studentId,
        HealthDataSourceType sourceType,
        double sleepHours,
        int lateNightCountPerWeek,
        int nutritionScore,
        int stressScore,
        int physicalActivityMinutesPerWeek,
        int infectionContacts,
        boolean feverReported,
        boolean coughReported,
        Instant observedAt,
        String note
) {
}