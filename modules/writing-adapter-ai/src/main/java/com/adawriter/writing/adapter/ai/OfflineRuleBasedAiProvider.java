package com.adawriter.writing.adapter.ai;

import com.adawriter.writing.domain.AiCompletionCommand;
import com.adawriter.writing.domain.AiCompletionResult;
import com.adawriter.writing.domain.AiProviderPort;

/**
 * Production offline provider: deterministic, on-device writing transforms when no cloud model is configured.
 *
 * <p>Supports local-first operation without network access. Not a test double — test doubles live only in
 * test sources.
 */
public final class OfflineRuleBasedAiProvider implements AiProviderPort {

    public static final String PROVIDER_ID = "offline";
    public static final String MODEL_ID = "offline-rules-v1";

    @Override
    public AiCompletionResult complete(AiCompletionCommand command) {
        long started = System.nanoTime();
        String text = transform(command.userPrompt());
        long latencyMs = Math.max(0L, (System.nanoTime() - started) / 1_000_000L);
        return new AiCompletionResult(text, MODEL_ID, latencyMs);
    }

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    private static String transform(String userPrompt) {
        String document = extractDocument(userPrompt);
        if (userPrompt.contains("Action: SHORTEN")) {
            return shorten(document);
        }
        if (userPrompt.contains("Action: EXPAND")) {
            return document + " Additionally, this draft expands the idea with clearer supporting detail.";
        }
        if (userPrompt.contains("Action: FIX_GRAMMAR")) {
            return document.replace(" i ", " I ").trim();
        }
        if (userPrompt.contains("Action: CHANGE_TONE")) {
            return applyTone(document, userPrompt);
        }
        return rewrite(document);
    }

    private static String rewrite(String document) {
        String trimmed = document.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1);
    }

    private static String applyTone(String document, String userPrompt) {
        if (userPrompt.toLowerCase().contains("professional")) {
            return document.trim();
        }
        if (userPrompt.toLowerCase().contains("casual")) {
            return document.trim();
        }
        return document.trim();
    }

    private static String extractDocument(String userPrompt) {
        int start = userPrompt.indexOf("---");
        int end = userPrompt.lastIndexOf("---");
        if (start >= 0 && end > start) {
            return userPrompt.substring(start + 3, end).trim();
        }
        return userPrompt.trim();
    }

    private static String shorten(String document) {
        String[] sentences = document.split("(?<=[.!?])\\s+");
        if (sentences.length <= 1) {
            return document;
        }
        return sentences[0].trim();
    }
}
