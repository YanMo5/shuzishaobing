package com.campushealth.platform.llm;

public record RagChunk(
        String source,
        String title,
        String content,
        double score
) {
}