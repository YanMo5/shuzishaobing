package com.campushealth.platform.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum HealthDataSourceType {
    CARD,
    DORMITORY,
    IOT,
    MANUAL,
    QUESTIONNAIRE,
    MODEL_PREDICTION;

    @JsonCreator
    public static HealthDataSourceType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "WEARABLE" -> IOT;
            case "CLINIC" -> CARD;
            default -> HealthDataSourceType.valueOf(normalized);
        };
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}