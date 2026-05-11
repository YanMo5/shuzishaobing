package com.campushealth.platform.llm;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.campushealth.platform.config.llm.CampusLlmProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class OpenAiCompatibleLlmClient implements ExternalLlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleLlmClient.class);

    private final CampusLlmProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().build();

    public OpenAiCompatibleLlmClient(CampusLlmProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<ExternalLlmResponse> generate(String systemPrompt, String userPrompt) {
        if (!properties.enabled() || properties.endpointUri() == null) {
            return Optional.empty();
        }

        try {
            URI uri = properties.endpointUri().resolve(properties.chatPathValue());
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("model", properties.modelName());
            payload.put("temperature", properties.temperatureValue());
            payload.put("max_tokens", properties.maxTokensValue());
            ArrayNode messages = payload.putArray("messages");
            messages.addObject().put("role", "system").put("content", systemPrompt);
            messages.addObject().put("role", "user").put("content", userPrompt);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(properties.timeout())
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)));
            if (properties.apiKey() != null && !properties.apiKey().isBlank()) {
                requestBuilder.header("Authorization", "Bearer " + properties.apiKey().trim());
            }

            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                log.warn("External LLM request failed: status={} body={}", response.statusCode(), response.body());
                return Optional.empty();
            }

            String rawContent = extractContent(response.body()).orElse(null);
            if (rawContent == null || rawContent.isBlank()) {
                log.warn("External LLM response did not contain usable content");
                return Optional.empty();
            }
            return Optional.of(new ExternalLlmResponse(properties.modelName(), rawContent));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("External LLM invocation interrupted at {}: {}", Instant.now(), exception.getMessage());
            return Optional.empty();
        } catch (IOException exception) {
            log.warn("External LLM invocation failed at {}: {}", Instant.now(), exception.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> extractContent(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode choices = root.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            JsonNode first = choices.get(0);
            JsonNode messageContent = first.path("message").path("content");
            if (!messageContent.isMissingNode() && !messageContent.isNull()) {
                return Optional.of(messageContent.asText());
            }
            JsonNode text = first.path("text");
            if (!text.isMissingNode() && !text.isNull()) {
                return Optional.of(text.asText());
            }
        }

        JsonNode outputText = root.path("output_text");
        if (!outputText.isMissingNode() && !outputText.isNull()) {
            return Optional.of(outputText.asText());
        }

        JsonNode content = root.path("content");
        if (content.isArray() && !content.isEmpty()) {
            JsonNode firstContent = content.get(0).path("text");
            if (!firstContent.isMissingNode() && !firstContent.isNull()) {
                return Optional.of(firstContent.asText());
            }
        }

        return Optional.empty();
    }
}