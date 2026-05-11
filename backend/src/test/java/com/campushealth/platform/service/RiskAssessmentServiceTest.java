package com.campushealth.platform.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.campushealth.platform.model.HealthObservation;
import com.campushealth.platform.model.RiskAssessment;
import com.campushealth.platform.model.RiskLevel;
import com.campushealth.platform.model.StudentProfile;

class RiskAssessmentServiceTest {

    private final RiskAssessmentService riskAssessmentService = new RiskAssessmentService();

    @Test
    void assessShouldMarkCriticalWhenMultipleRisksAppearTogether() {
        StudentProfile student = new StudentProfile("S9999", "测试学生", "测试学院", "测试专业", "测试班级", 2, "T1-101");
        HealthObservation observation = new HealthObservation(4.5, 5, 40, 85, 30, 2, true, true);

        RiskAssessment assessment = riskAssessmentService.assess(student, observation);

        assertEquals(RiskLevel.CRITICAL, assessment.riskLevel());
        assertTrue(assessment.riskScore() >= 80);
        assertTrue(assessment.riskFactors().contains("睡眠严重不足"));
        assertTrue(assessment.riskFactors().contains("心理压力过高"));
    }
}
