package com.campushealth.platform.llm;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.campushealth.platform.dto.ModelInferenceRequest;
import com.campushealth.platform.dto.StudentSummaryResponse;
import com.campushealth.platform.model.CampusHealthSignal;
import com.campushealth.platform.model.RiskAssessment;
import com.campushealth.platform.model.StudentProfile;

@Service
public class CampusDeidentificationService {

    private static final Pattern STUDENT_ID_PATTERN = Pattern.compile("S\\d{4,}");
    private static final Pattern SUBMITTED_BY_PATTERN = Pattern.compile("submitted-by=[^|\\s]+");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public DeidentifiedPromptContext buildContext(StudentSummaryResponse summary,
                                                  CampusHealthSignal latestSignal,
                                                  ModelInferenceRequest request,
                                                  List<RagChunk> chunks) {
        StudentProfile student = summary.student();
        RiskAssessment assessment = summary.assessment();
        String subjectAlias = aliasStudentId(student.studentId());
        String profileSummary = "学生别名=" + subjectAlias
                + "，学院=" + student.college()
                + "，专业=" + student.major()
                + "，年级=" + student.grade()
                + "。";
        String observationSummary = "睡眠=" + summary.observation().sleepHours()
                + "h，熬夜次数/周=" + summary.observation().lateNightCountPerWeek()
                + "，营养分数=" + summary.observation().nutritionScore()
                + "，压力分数=" + summary.observation().stressScore()
                + "，活动分钟/周=" + summary.observation().physicalActivityMinutesPerWeek()
                + "，感染接触人数=" + summary.observation().infectionContacts()
                + "，发热=" + summary.observation().feverReported()
                + "，咳嗽=" + summary.observation().coughReported() + "。";
        String riskSummary = "风险等级=" + assessment.riskLevel()
                + "，评分=" + assessment.riskScore()
                + "，风险因子=" + String.join("、", assessment.riskFactors().isEmpty() ? List.of("暂无显著异常") : assessment.riskFactors())
                + "; 干预建议=" + String.join("、", assessment.interventionPlan().immediateActions()) + "。";
        String signalSummary = latestSignal == null ? "暂无最近一条原始信号。" : buildSignalSummary(latestSignal);
        String retrievalSummary = chunks.isEmpty()
                ? "未命中可检索知识片段。"
                : chunks.stream().map(chunk -> "[" + chunk.source() + "#" + chunk.title() + "] " + chunk.content()).reduce((left, right) -> left + "\n\n" + right).orElse("未命中可检索知识片段。");
        String querySummary = buildQuerySummary(request, assessment);
        return new DeidentifiedPromptContext(subjectAlias, profileSummary, observationSummary, riskSummary, signalSummary, chunks, retrievalSummary, querySummary);
    }

    public String sanitizeText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String sanitized = SUBMITTED_BY_PATTERN.matcher(text).replaceAll("submitted-by=[REDACTED]");
        sanitized = STUDENT_ID_PATTERN.matcher(sanitized).replaceAll("S****");
        return sanitized;
    }

    public String aliasStudentId(String studentId) {
        if (studentId == null || studentId.isBlank()) {
            return "student-unknown";
        }
        String normalized = studentId.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() <= 3) {
            return normalized;
        }
        String suffix = normalized.substring(Math.max(0, normalized.length() - 2));
        return "student-" + suffix;
    }

    private String buildSignalSummary(CampusHealthSignal signal) {
        return "来源=" + signal.sourceType()
                + "，观测时间=" + TIME_FORMATTER.format(signal.observedAt().atOffset(java.time.ZoneOffset.UTC))
                + "，睡眠=" + signal.sleepHours()
                + "h，压力=" + signal.stressScore()
                + "，营养=" + signal.nutritionScore()
                + "，活动分钟/周=" + signal.physicalActivityMinutesPerWeek()
                + "，接触感染人数=" + signal.infectionContacts()
                + "，备注=" + sanitizeText(signal.note()) + ".";
    }

    private String buildQuerySummary(ModelInferenceRequest request, RiskAssessment assessment) {
        StringBuilder builder = new StringBuilder();
        builder.append(request.prompt() == null || request.prompt().isBlank() ? "生成校园健康干预建议" : request.prompt().trim());
        if (request.focus() != null && !request.focus().isBlank()) {
            builder.append("；聚焦方向=").append(request.focus().trim());
        }
        builder.append("；风险等级=").append(assessment.riskLevel());
        builder.append("；风险分数=").append(assessment.riskScore());
        return builder.toString();
    }
}