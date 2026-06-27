package com.org.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Guards incoming user messages against prompt injection before they reach the LLM.
 *
 * <p>Patterns are externalised in {@link InjectionGuardProperties}
 * ({@code app.security.injection-guard.patterns}) so new attack signatures can be added
 * in configuration without code changes.</p>
 *
 * <p>Call {@link #isQuerySafe(String)} on every user message before passing it to
 * {@code ChatClient}. Return {@link #blockMessage()} to the caller if it returns {@code false}.</p>
 */
@Slf4j
@Component
public class PromptInjectionGuard {

    private final List<Pattern> compiledPatterns;
    private final boolean enabled;
    private final String blockMessage;

    public PromptInjectionGuard(InjectionGuardProperties properties) {
        this.enabled = properties.isEnabled();
        this.blockMessage = properties.getBlockMessage();
        this.compiledPatterns = properties.getPatterns().stream()
                .flatMap(regex -> {
                    try {
                        return java.util.stream.Stream.of(Pattern.compile(regex));
                    } catch (PatternSyntaxException ex) {
                        log.error("SECURITY | invalid injection pattern skipped | regex='{}' | error={}", regex, ex.getMessage());
                        return java.util.stream.Stream.empty();
                    }
                })
                .toList();
        log.info("SECURITY | PromptInjectionGuard ready | patterns={} enabled={}", compiledPatterns.size(), enabled);
    }

    /**
     * Returns {@code true} if the text matches none of the configured injection patterns.
     */
    public boolean isSafe(String text) {
        if (!enabled || text == null || text.isBlank()) return true;
        return compiledPatterns.stream().noneMatch(p -> p.matcher(text).find());
    }

    /**
     * Validates the user's message before it enters the LLM pipeline.
     * Returns {@code false} if an injection pattern is detected.
     */
    public boolean isQuerySafe(String userMessage) {
        boolean safe = isSafe(userMessage);
        if (!safe) {
            log.warn("SECURITY | Injection pattern detected in user message — rejecting request");
        }
        return safe;
    }

    /** The configured rejection message to return to the caller when {@link #isQuerySafe} returns {@code false}. */
    public String blockMessage() {
        return blockMessage;
    }
}
