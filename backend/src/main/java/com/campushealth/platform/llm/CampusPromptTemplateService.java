package com.campushealth.platform.llm;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import com.campushealth.platform.config.llm.CampusLlmProperties;

@Service
public class CampusPromptTemplateService {

    private final String systemTemplate;
    private final String userTemplate;

    public CampusPromptTemplateService(CampusLlmProperties properties) {
        this.systemTemplate = loadTemplate(properties.systemTemplatePath(), defaultSystemTemplate());
        this.userTemplate = loadTemplate(properties.userTemplatePath(), defaultUserTemplate());
    }

    public String buildSystemPrompt() {
        return systemTemplate;
    }

    public String buildUserPrompt(DeidentifiedPromptContext context) {
        String retrievalBlock = context.retrievedChunks().isEmpty()
                ? "- 无命中"
                : context.retrievedChunks().stream()
                        .map(chunk -> "- 来源: " + chunk.source() + " | 标题: " + chunk.title() + " | 片段: " + chunk.content())
                        .reduce((left, right) -> left + "\n" + right)
                        .orElse("- 无命中");

        return userTemplate
                .replace("{{subject_alias}}", context.subjectAlias())
                .replace("{{profile_summary}}", context.profileSummary())
                .replace("{{observation_summary}}", context.observationSummary())
                .replace("{{risk_summary}}", context.riskSummary())
                .replace("{{signal_summary}}", context.signalSummary())
                .replace("{{retrieval_summary}}", context.retrievalSummary())
                .replace("{{retrieval_block}}", retrievalBlock)
                .replace("{{query_summary}}", context.querySummary())
                .replace("{{output_schema}}", outputSchema());
    }

    public String outputSchema() {
        return "{\"narrative\":\"...\",\"confidence\":0.87,\"recommended_actions\":[\"...\"],\"key_evidence\":[\"...\"]}";
    }

    private String loadTemplate(String templateLocation, String fallback) {
        if (templateLocation == null || templateLocation.isBlank()) {
            return fallback;
        }

        try {
            Resource resource = new PathMatchingResourcePatternResolver().getResource(templateLocation);
            if (resource.exists()) {
                return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load prompt template: " + templateLocation, exception);
        }
        return fallback;
    }

    private String defaultSystemTemplate() {
        return String.join("\n",
                "你是校园健康平台的外部大模型推理助手。",
                "只能基于去标识化输入和检索到的知识片段输出结论。",
                "不要输出姓名、学号、宿舍号或任何可以重新识别学生的个人信息。",
                "如果证据不足，要明确说明不确定性，不要编造事实。",
                "只输出严格 JSON，不要附加解释文字。",
                "JSON 字段必须包含 narrative、confidence、recommended_actions、key_evidence。"
        );
    }

    private String defaultUserTemplate() {
        return String.join("\n",
                "任务: {{query_summary}}",
                "主体别名: {{subject_alias}}",
                "去标识化画像: {{profile_summary}}",
                "最新观测: {{observation_summary}}",
                "风险摘要: {{risk_summary}}",
                "最新信号: {{signal_summary}}",
                "检索到的知识片段:\n{{retrieval_block}}",
                "知识摘要: {{retrieval_summary}}",
                "输出要求: {{output_schema}}"
        );
    }
}