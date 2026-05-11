package com.campushealth.platform.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.campushealth.platform.config.llm.CampusLlmProperties;
import com.campushealth.platform.dto.ModelInferenceRequest;
import com.campushealth.platform.dto.ModelInferenceResponse;
import com.campushealth.platform.dto.StudentSummaryResponse;
import com.campushealth.platform.llm.CampusDeidentificationService;
import com.campushealth.platform.llm.CampusPromptTemplateService;
import com.campushealth.platform.llm.CampusRagService;
import com.campushealth.platform.llm.DeidentifiedPromptContext;
import com.campushealth.platform.llm.ExternalLlmClient;
import com.campushealth.platform.llm.ExternalLlmResponse;
import com.campushealth.platform.llm.RagChunk;
import com.campushealth.platform.model.CampusHealthSignal;
import com.campushealth.platform.model.RiskAssessment;
import com.campushealth.platform.model.StudentProfile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ModelInferenceService {

    private static final Logger log = LoggerFactory.getLogger(ModelInferenceService.class);
    private static final String FALLBACK_MODEL_NAME = "campus-health-rule-llm-v1";

    private final StudentHealthService studentHealthService;
    private final CampusDataIngestionService campusDataIngestionService;
    private final CampusLlmProperties llmProperties;
    private final CampusDeidentificationService deidentificationService;
    private final CampusPromptTemplateService promptTemplateService;
    private final CampusRagService ragService;
    private final ExternalLlmClient externalLlmClient;
    private final ObjectMapper objectMapper;

    public ModelInferenceService(StudentHealthService studentHealthService,
                                 CampusDataIngestionService campusDataIngestionService,
                                 CampusLlmProperties llmProperties,
                                 CampusDeidentificationService deidentificationService,
                                 CampusPromptTemplateService promptTemplateService,
                                 CampusRagService ragService,
                                 ExternalLlmClient externalLlmClient,
                                 ObjectMapper objectMapper) {
        this.studentHealthService = studentHealthService;
        this.campusDataIngestionService = campusDataIngestionService;
        this.llmProperties = llmProperties;
        this.deidentificationService = deidentificationService;
        this.promptTemplateService = promptTemplateService;
        this.ragService = ragService;
        this.externalLlmClient = externalLlmClient;
        this.objectMapper = objectMapper;
    }

    public ModelInferenceResponse infer(ModelInferenceRequest request) {
        StudentSummaryResponse summary = studentHealthService.getSummary(request.studentId());
        RiskAssessment riskAssessment = summary.assessment();
        CampusHealthSignal latestSignal = campusDataIngestionService.latestSignal(request.studentId()).orElse(null);
        String retrievalQuery = buildRetrievalQuery(request, summary, latestSignal);
        List<RagChunk> retrievedChunks = ragService.retrieve(retrievalQuery, llmProperties.maxContextSnippetsValue());
        DeidentifiedPromptContext promptContext = deidentificationService.buildContext(summary, latestSignal, request, retrievedChunks);
        String systemPrompt = promptTemplateService.buildSystemPrompt();
        String userPrompt = promptTemplateService.buildUserPrompt(promptContext);

        StructuredInference inference = externalLlmClient.generate(systemPrompt, userPrompt)
                .flatMap(this::parseStructuredInference)
                .orElseGet(() -> buildFallbackInference(summary.student(), latestSignal, riskAssessment, request, promptContext));

        List<String> actions = mergeActions(inference.recommendedActions(), riskAssessment);
        String modelName = inference.modelName() == null || inference.modelName().isBlank()
                ? (llmProperties.enabled() ? llmProperties.modelName() : FALLBACK_MODEL_NAME)
                : inference.modelName();
        String prompt = request.prompt() == null || request.prompt().isBlank()
                ? "生成校园健康干预建议"
                : request.prompt().trim();
        String narrative = inference.narrative() == null || inference.narrative().isBlank()
                ? buildFallbackNarrative(summary.student(), latestSignal, riskAssessment, request, promptContext)
                : inference.narrative();

        return new ModelInferenceResponse(
                request.studentId(),
                modelName,
                prompt,
                narrative,
                normalizeConfidence(inference.confidence(), riskAssessment, latestSignal),
                actions,
                riskAssessment,
                Instant.now()
        );
    }

    private StructuredInference buildFallbackInference(StudentProfile student,
                                                       CampusHealthSignal latestSignal,
                                                       RiskAssessment riskAssessment,
                                                       ModelInferenceRequest request,
                                                       DeidentifiedPromptContext promptContext) {
        return new StructuredInference(
                FALLBACK_MODEL_NAME,
                buildFallbackNarrative(student, latestSignal, riskAssessment, request, promptContext),
                confidence(riskAssessment, latestSignal),
                mergeActions(List.of(), riskAssessment)
        );
    }

    private String buildFallbackNarrative(StudentProfile student,
                                          CampusHealthSignal latestSignal,
                                          RiskAssessment riskAssessment,
                                          ModelInferenceRequest request,
                                          DeidentifiedPromptContext promptContext) {
        StringBuilder builder = new StringBuilder();
        builder.append("模型任务: ").append(request.prompt() == null || request.prompt().isBlank() ? "生成校园健康干预建议" : request.prompt().trim()).append("。 ");
        if (request.focus() != null && !request.focus().isBlank()) {
            builder.append("聚焦方向: ").append(request.focus().trim()).append("。 ");
        }
        builder.append("主体别名: ").append(promptContext.subjectAlias()).append("。 ");
        builder.append("学生 ").append(student.name()).append(" 当前风险等级为 ").append(riskAssessment.riskLevel())
                .append("，评分 ").append(riskAssessment.riskScore()).append("。 ");
        if (latestSignal != null) {
            builder.append("最近一次来自 ").append(latestSignal.sourceType())
                    .append(" 的数据已接入，观测时间 ").append(latestSignal.observedAt()).append("。 ");
        }
        builder.append("建议优先处理的风险因子包括 ")
                .append(String.join("、", riskAssessment.riskFactors().isEmpty() ? List.of("暂无显著异常") : riskAssessment.riskFactors()))
                .append("。");
        if (!promptContext.retrievedChunks().isEmpty()) {
            builder.append("检索到的知识片段包括 ")
                    .append(promptContext.retrievedChunks().stream().map(chunk -> chunk.title()).distinct().reduce((left, right) -> left + "、" + right).orElse("暂无"))
                    .append("。 ");
        }
        return builder.toString();
    }

    private Optional<StructuredInference> parseStructuredInference(ExternalLlmResponse response) {
        try {
            JsonNode root = objectMapper.readTree(stripCodeFences(response.rawContent()));
            String narrative = firstText(root, "narrative", "summary", "answer");
            Double confidence = firstDouble(root, "confidence", "score");
            List<String> recommendedActions = firstStringList(root, "recommended_actions", "recommendedActions");
            return Optional.of(new StructuredInference(response.modelName(), narrative, confidence, recommendedActions));
        } catch (java.io.IOException exception) {
            log.warn("Failed to parse structured LLM response: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    private String stripCodeFences(String content) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        return trimmed;
    }

    private String firstText(JsonNode root, String... candidates) {
        for (String candidate : candidates) {
            JsonNode node = root.path(candidate);
            if (!node.isMissingNode() && !node.isNull() && !node.asText().isBlank()) {
                return node.asText();
            }
        }
        return null;
    }

    private Double firstDouble(JsonNode root, String... candidates) {
        for (String candidate : candidates) {
            JsonNode node = root.path(candidate);
            if (!node.isMissingNode() && !node.isNull() && node.isNumber()) {
                return node.asDouble();
            }
            if (!node.isMissingNode() && !node.isNull()) {
                try {
                    return Double.valueOf(node.asText());
                } catch (NumberFormatException ignored) {
                    // Ignore and continue.
                }
            }
        }
        return null;
    }

    private List<String> firstStringList(JsonNode root, String... candidates) {
        for (String candidate : candidates) {
            JsonNode node = root.path(candidate);
            if (node.isArray()) {
                List<String> values = new ArrayList<>();
                for (JsonNode item : node) {
                    if (!item.asText().isBlank()) {
                        values.add(item.asText());
                    }
                }
                if (!values.isEmpty()) {
                    return values;
                }
            }
        }
        return List.of();
    }

    private List<String> mergeActions(List<String> modelActions, RiskAssessment riskAssessment) {
        List<String> actions = new ArrayList<>();
        if (modelActions != null) {
            for (String action : modelActions) {
                if (action != null && !action.isBlank() && !actions.contains(action)) {
                    actions.add(action.trim());
                }
            }
        }
        for (String action : riskAssessment.interventionPlan().immediateActions()) {
            if (!actions.contains(action)) {
                actions.add(action);
            }
        }
        for (String action : riskAssessment.interventionPlan().followUpActions()) {
            if (!actions.contains(action)) {
                actions.add(action);
            }
        }
        return actions;
    }

    private String buildRetrievalQuery(ModelInferenceRequest request, StudentSummaryResponse summary, CampusHealthSignal latestSignal) {
        StringBuilder builder = new StringBuilder();
        builder.append(request.prompt() == null ? "" : request.prompt()).append(' ');
        builder.append(request.focus() == null ? "" : request.focus()).append(' ');
        builder.append(summary.assessment().riskLevel()).append(' ');
        builder.append(summary.assessment().riskScore()).append(' ');
        builder.append(String.join(" ", summary.assessment().riskFactors()));
        if (latestSignal != null) {
            builder.append(' ').append(latestSignal.sourceType());
            builder.append(' ').append(latestSignal.note() == null ? "" : latestSignal.note());
        }
        return builder.toString();
    }

    private double confidence(RiskAssessment riskAssessment, CampusHealthSignal latestSignal) {
        double base = switch (riskAssessment.riskLevel()) {
            case LOW -> 0.78;
            case MEDIUM -> 0.84;
            case HIGH -> 0.91;
            case CRITICAL -> 0.95;
        };
        if (latestSignal != null) {
            base += 0.02;
        }
        return Math.min(base, 0.99);
    }

    private double normalizeConfidence(Double confidence, RiskAssessment riskAssessment, CampusHealthSignal latestSignal) {
        if (confidence == null || confidence.isNaN()) {
            return confidence(riskAssessment, latestSignal);
        }
        double normalized = confidence;
        if (normalized > 1.0d) {
            normalized = normalized / 100.0d;
        }
        if (normalized < 0d) {
            normalized = 0d;
        }
        if (normalized > 0.99d) {
            normalized = 0.99d;
        }
        return normalized;
    }

    private record StructuredInference(
            String modelName,
            String narrative,
            Double confidence,
            List<String> recommendedActions
    ) {
    }
}