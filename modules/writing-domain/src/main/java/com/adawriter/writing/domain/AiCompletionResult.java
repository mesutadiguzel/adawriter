package com.adawriter.writing.domain;

import java.util.Objects;

/**
 * Provider-agnostic completion result.
 */
public record AiCompletionResult(String text, String modelId, long latencyMs) {

    public AiCompletionResult {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(modelId, "modelId");
        if (latencyMs < 0) {
            throw new ValidationException("latencyMs must be >= 0");
        }
    }
}
