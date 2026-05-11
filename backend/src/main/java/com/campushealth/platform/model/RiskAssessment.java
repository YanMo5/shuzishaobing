package com.campushealth.platform.model;

import java.time.Instant;
import java.util.List;

public record RiskAssessment(
        String studentId,
        String studentName,
        RiskLevel riskLevel,
        int riskScore,
        List<String> riskFactors,
        InterventionPlan interventionPlan,
        Instant assessedAt
) {
}
