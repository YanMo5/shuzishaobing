package com.campushealth.platform.llm;

import java.util.Optional;

public interface ExternalLlmClient {

    Optional<ExternalLlmResponse> generate(String systemPrompt, String userPrompt);
}