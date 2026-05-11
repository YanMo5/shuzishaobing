package com.campushealth.platform.llm;

import java.util.List;

public record DeidentifiedPromptContext(
        String subjectAlias,
        String profileSummary,
        String observationSummary,
        String riskSummary,
        String signalSummary,
        List<RagChunk> retrievedChunks,
        String retrievalSummary,
        String querySummary
) {
}