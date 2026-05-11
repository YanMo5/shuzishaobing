package com.campushealth.platform.config.llm;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "campus.llm")
public class CampusLlmProperties {

    private boolean enabled = false;
    private String provider = "openai-compatible";
    private String endpointUrl = "";
    private String chatPath = "/v1/chat/completions";
    private String apiKey = "";
    private String model = "gpt-4o-mini";
    private double temperature = 0.2d;
    private int maxTokens = 800;
    private int timeoutSeconds = 20;
    private String systemTemplate = "classpath:/prompts/campus-health-system.txt";
    private String userTemplate = "classpath:/prompts/campus-health-user.txt";
    private int maxContextSnippets = 3;

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String provider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String endpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public String chatPath() {
        return chatPath;
    }

    public void setChatPath(String chatPath) {
        this.chatPath = chatPath;
    }

    public String apiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String model() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public double temperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int maxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public int timeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public String systemTemplate() {
        return systemTemplate;
    }

    public void setSystemTemplate(String systemTemplate) {
        this.systemTemplate = systemTemplate;
    }

    public String userTemplate() {
        return userTemplate;
    }

    public void setUserTemplate(String userTemplate) {
        this.userTemplate = userTemplate;
    }

    public int maxContextSnippets() {
        return maxContextSnippets;
    }

    public void setMaxContextSnippets(int maxContextSnippets) {
        this.maxContextSnippets = maxContextSnippets;
    }

    public URI endpointUri() {
        if (endpointUrl == null || endpointUrl.isBlank()) {
            return null;
        }
        return URI.create(endpointUrl.trim());
    }

    public String providerName() {
        return provider == null || provider.isBlank() ? "openai-compatible" : provider.trim();
    }

    public String modelName() {
        return model == null || model.isBlank() ? "gpt-4o-mini" : model.trim();
    }

    public String chatPathValue() {
        return chatPath == null || chatPath.isBlank() ? "/v1/chat/completions" : chatPath.trim();
    }

    public double temperatureValue() {
        return temperature;
    }

    public int maxTokensValue() {
        return maxTokens < 1 ? 800 : maxTokens;
    }

    public Duration timeout() {
        return Duration.ofSeconds(timeoutSeconds < 1 ? 20 : timeoutSeconds);
    }

    public String systemTemplatePath() {
        return systemTemplate == null || systemTemplate.isBlank()
                ? "classpath:/prompts/campus-health-system.txt"
                : systemTemplate.trim();
    }

    public String userTemplatePath() {
        return userTemplate == null || userTemplate.isBlank()
                ? "classpath:/prompts/campus-health-user.txt"
                : userTemplate.trim();
    }

    public int maxContextSnippetsValue() {
        return maxContextSnippets < 1 ? 3 : maxContextSnippets;
    }
}