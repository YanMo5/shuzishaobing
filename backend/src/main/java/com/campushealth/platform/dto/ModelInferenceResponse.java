package com.campushealth.platform.dto;

import java.time.Instant;
import java.util.List;

import com.campushealth.platform.model.RiskAssessment;

public record ModelInferenceResponse(
        String studentId,
        String modelName,
        String prompt,
        String narrative,
        double confidence,
        List<String> recommendedActions,
        RiskAssessment riskAssessment,
        Instant generatedAt
) {
}