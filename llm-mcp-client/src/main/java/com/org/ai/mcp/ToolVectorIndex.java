package com.org.ai.mcp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Indexes every available MCP tool definition in the pgvector vector store at startup.
 * This enables the {@link SemanticToolSelector} to retrieve only the K most relevant
 * tools for a given user query instead of flooding the LLM context with all tool schemas.
 * <p>
 * Re-runs on each restart so stale tool entries (from servers that are no longer reachable)
 * are replaced with the current live set.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolVectorIndex {

    private final VectorStore vectorStore;
    private final ToolCallbackProvider toolCallbackProvider;

    @EventListener(ApplicationReadyEvent.class)
    public void indexTools() {
        ToolCallback[] callbacks = toolCallbackProvider.getToolCallbacks();
        if (callbacks.length == 0) {
            log.warn("No MCP tools available to index — vector store will be empty");
            return;
        }

        // Delete previous entries so restarts don't leave stale tool schemas
        List<String> ids = Arrays.stream(callbacks)
                .map(cb -> cb.getToolDefinition().name())
                .toList();
        vectorStore.delete(ids);

        List<Document> docs = new ArrayList<>(callbacks.length);
        for (ToolCallback cb : callbacks) {
            String name = cb.getToolDefinition().name();
            String description = cb.getToolDefinition().description();
            String schema = cb.getToolDefinition().inputSchema();

            // Pack name + description + parameter schema into the embedded text so
            // semantic search finds tools by intent, not just keyword.
            String content = "Tool: " + name
                    + "\nDescription: " + description
                    + "\nParameters: " + schema;

            docs.add(Document.builder()
                    .id(name)
                    .text(content)
                    .metadata(Map.of("tool_name", name))
                    .build());
        }

        vectorStore.add(docs);
        log.info("Indexed {} MCP tool definitions in vector store", docs.size());
    }
}
