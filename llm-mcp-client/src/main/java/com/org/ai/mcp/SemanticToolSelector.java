package com.org.ai.mcp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Selects the top-K most relevant MCP tools for a given user query via semantic
 * similarity search against the pgvector index built by {@link ToolVectorIndex}.
 *
 * <p>This prevents context explosion: instead of injecting every tool schema into
 * every LLM call, only the K schemas that best match the current intent are included.
 * Falls back to the full tool set if the vector store returns no results (e.g. on
 * first boot before indexing completes, or if the store is temporarily unavailable).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SemanticToolSelector {

    private final VectorStore vectorStore;
    private final ToolCallbackProvider toolCallbackProvider;

    @Value("${assistant.tool-selector.top-k:10}")
    private int topK;

    /**
     * Returns the subset of available {@link ToolCallback}s whose descriptions are
     * semantically closest to {@code userQuery}.
     */
    public ToolCallback[] selectTools(String userQuery) {
        List<Document> matches = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(userQuery)
                        .topK(topK)
                        .build());

        Set<String> selectedNames = matches.stream()
                .map(d -> (String) d.getMetadata().get("tool_name"))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        ToolCallback[] all = toolCallbackProvider.getToolCallbacks();
        ToolCallback[] selected = Arrays.stream(all)
                .filter(cb -> selectedNames.contains(cb.getToolDefinition().name()))
                .toArray(ToolCallback[]::new);

        String preview = userQuery.length() > 80 ? userQuery.substring(0, 80) + "…" : userQuery;
        log.info("semantic-tool-selection query='{}' total={} selected={} tools={}",
                preview, all.length, selected.length, selectedNames);

        // If the vector store has no results yet (cold start), use all available tools
        return selected.length > 0 ? selected : all;
    }
}
