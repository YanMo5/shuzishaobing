package com.campushealth.platform.config.llm;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "campus.rag")
public class CampusRagProperties {
    private boolean enabled = true;
    private List<String> sources = new java.util.ArrayList<>(List.of("classpath:/rag/campus-health-knowledge.md"));
    private int maxResults = 3;
    private int maxCharsPerChunk = 800;

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> sources() {
        return sources;
    }

    public void setSources(List<String> sources) {
        if (sources == null || sources.isEmpty()) {
            this.sources = new java.util.ArrayList<>(List.of("classpath:/rag/campus-health-knowledge.md"));
            return;
        }
        this.sources = new java.util.ArrayList<>(sources);
    }

    public int maxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public int maxCharsPerChunk() {
        return maxCharsPerChunk;
    }

    public void setMaxCharsPerChunk(int maxCharsPerChunk) {
        this.maxCharsPerChunk = maxCharsPerChunk;
    }

    public List<String> resolvedSources() {
        return sources == null || sources.isEmpty()
                ? List.of("classpath:/rag/campus-health-knowledge.md")
                : sources;
    }

    public int maxResultsValue() {
        return maxResults < 1 ? 3 : maxResults;
    }

    public int maxCharsPerChunkValue() {
        return maxCharsPerChunk < 120 ? 800 : maxCharsPerChunk;
    }
}