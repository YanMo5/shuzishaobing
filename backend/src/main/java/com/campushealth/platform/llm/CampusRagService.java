package com.campushealth.platform.llm;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import com.campushealth.platform.config.llm.CampusRagProperties;

import jakarta.annotation.PostConstruct;

@Service
public class CampusRagService {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\p{L}\\p{N}]{2,}");

    private final CampusRagProperties properties;
    private final List<RagChunk> chunks = new ArrayList<>();

    public CampusRagService(CampusRagProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void load() {
        if (!properties.enabled()) {
            return;
        }

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        for (String source : properties.resolvedSources()) {
            try {
                Resource[] resources = resolver.getResources(source);
                for (Resource resource : resources) {
                    if (!resource.exists()) {
                        continue;
                    }
                    String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
                    chunks.addAll(splitResource(resource, content));
                }
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to load RAG source: " + source, exception);
            }
        }
    }

    public List<RagChunk> retrieve(String query, int limit) {
        if (!properties.enabled() || chunks.isEmpty() || query == null || query.isBlank()) {
            return List.of();
        }

        Set<String> queryTokens = tokenize(query);
        int effectiveLimit = Math.max(1, Math.min(limit, properties.maxResultsValue()));
        return chunks.stream()
                .map(chunk -> new RagChunk(chunk.source(), chunk.title(), chunk.content(), score(queryTokens, chunk.content())))
                .filter(chunk -> chunk.score() > 0d)
                .sorted(Comparator.comparingDouble(RagChunk::score).reversed())
                .limit(effectiveLimit)
                .toList();
    }

    private List<RagChunk> splitResource(Resource resource, String content) {
        List<RagChunk> result = new ArrayList<>();
        String[] blocks = content.split("\\R{2,}");
        String source = resource.getFilename() == null ? resource.getDescription() : resource.getFilename();
        String currentTitle = source;
        for (String block : blocks) {
            String trimmed = block.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            String title = currentTitle;
            Matcher heading = Pattern.compile("(?m)^#{1,6}\\s+(.+)$").matcher(trimmed);
            if (heading.find()) {
                title = heading.group(1).trim();
            }
            String normalized = trimmed.length() > properties.maxCharsPerChunkValue()
                    ? trimmed.substring(0, properties.maxCharsPerChunkValue())
                    : trimmed;
            result.add(new RagChunk(source, title, normalized, 0d));
            currentTitle = title;
        }
        if (result.isEmpty() && !content.isBlank()) {
            result.add(new RagChunk(source, currentTitle, content.substring(0, Math.min(properties.maxCharsPerChunkValue(), content.length())), 0d));
        }
        return result;
    }

    private double score(Set<String> queryTokens, String content) {
        Set<String> contentTokens = tokenize(content);
        if (contentTokens.isEmpty() || queryTokens.isEmpty()) {
            return 0d;
        }
        Set<String> overlap = new HashSet<>(queryTokens);
        overlap.retainAll(contentTokens);
        return overlap.size() + Arrays.stream(new String[]{"睡眠", "压力", "营养", "感染", "干预", "风险", "审计", "学生端", "管理台"})
                .filter(content::contains)
                .filter(queryTokens::contains)
                .count() * 0.5d;
    }

    private Set<String> tokenize(String text) {
        Set<String> tokens = new HashSet<>();
        Matcher matcher = TOKEN_PATTERN.matcher(text);
        while (matcher.find()) {
            tokens.add(matcher.group().toLowerCase());
        }
        return tokens;
    }
}