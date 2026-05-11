package com.campushealth.platform.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import com.campushealth.platform.dto.CampusHealthSignalRequest;
import com.campushealth.platform.dto.ModelInferenceRequest;
import com.campushealth.platform.dto.ModelInferenceResponse;
import com.campushealth.platform.dto.StudentRegistrationRequest;
import com.campushealth.platform.dto.StudentRegistrationResponse;
import com.campushealth.platform.dto.StudentSummaryResponse;
import com.campushealth.platform.model.CampusPrincipal;
import com.campushealth.platform.model.HealthDataSourceType;
import com.campushealth.platform.model.RiskLevel;

@SpringBootTest
@ActiveProfiles("test")
class CampusPlatformServiceTest {

    @Autowired
    private CampusPlatformService campusPlatformService;

    @Autowired
    private AccessControlService accessControlService;

    @Test
    void staffCanIngestAndInspectStudentData() {
        CampusPrincipal staff = accessControlService.resolvePrincipal("token-staff-health").orElseThrow();
        CampusHealthSignalRequest request = new CampusHealthSignalRequest(
                HealthDataSourceType.QUESTIONNAIRE,
                4.8,
                4,
                42,
                86,
                25,
                2,
                true,
                true,
                "夜间症状明显"
        );

        campusPlatformService.ingestSignal("S1001", request, staff);
        StudentSummaryResponse summary = campusPlatformService.getSummary("S1001", staff);

        assertEquals(RiskLevel.CRITICAL, summary.assessment().riskLevel());
        assertTrue(summary.assessment().riskFactors().contains("心理压力过高"));
        assertFalse(campusPlatformService.getSignals("S1001", staff).isEmpty());
    }

    @Test
    void studentCannotAccessOtherStudentsRecord() {
        CampusPrincipal student = accessControlService.resolvePrincipal("token-student-s1001").orElseThrow();

        try {
            campusPlatformService.getSummary("S1002", student);
        } catch (ResponseStatusException exception) {
            assertEquals(403, exception.getStatusCode().value());
            return;
        }
        throw new AssertionError("Expected ResponseStatusException");
    }

    @Test
    void inferenceReturnsNarrativeAndRecommendedActions() {
        CampusPrincipal staff = accessControlService.resolvePrincipal("token-staff-health").orElseThrow();
        ModelInferenceResponse response = campusPlatformService.infer(
                new ModelInferenceRequest("S1001", "生成健康风险解释", "sleep"),
                staff
        );

        assertEquals("S1001", response.studentId());
        assertTrue(response.narrative().contains("风险等级"));
        assertTrue(response.confidence() > 0.7);
        assertFalse(response.recommendedActions().isEmpty());
    }

    @Test
    void adminCanReadAuditTrail() {
        CampusPrincipal staff = accessControlService.resolvePrincipal("token-staff-health").orElseThrow();
        CampusPrincipal admin = accessControlService.resolvePrincipal("token-admin-platform").orElseThrow();

        campusPlatformService.getSummary("S1001", staff);
        assertFalse(campusPlatformService.listAuditEvents(admin).isEmpty());
    }

    @Test
    void studentRegistrationProducesLoginToken() {
        StudentRegistrationResponse response = campusPlatformService.registerStudent(
                new StudentRegistrationRequest("测试学生", "S2099", "test123", "测试学院", "测试专业", "测试班级", 2, "T1-101")
        );

        assertEquals("token-student-s2099", response.token());
        assertEquals("S2099", response.student().studentId());
        assertEquals("测试学生", response.student().name());

        CampusPrincipal principal = accessControlService.resolvePrincipal(response.token()).orElseThrow();
        assertEquals("S2099", principal.studentId());
        assertTrue(principal.isStudent());
    }

    @Test
    void staffCanResetStudentPassword() {
        CampusPrincipal staff = accessControlService.resolvePrincipal("token-staff-health").orElseThrow();

        campusPlatformService.resetStudentPasswordByStaff("S1001", "reset456", staff);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> campusPlatformService.loginStudent("S1001", "pass123"));
        assertTrue(exception.getMessage().contains("学号或密码错误"));
        assertEquals("token-student-s1001", campusPlatformService.loginStudent("S1001", "reset456").token());
    }
}