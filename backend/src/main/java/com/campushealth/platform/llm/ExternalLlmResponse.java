package com.campushealth.platform.llm;

public record ExternalLlmResponse(
        String modelName,
        String rawContent
) {
}