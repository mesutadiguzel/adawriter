package com.adawriter.writing.domain;

import java.util.Objects;

/**
 * Immutable result of a writing assistance call.
 */
public record WritingResult(
        String outputText, String providerId, String modelId, String promptVersion, long latencyMs) {

    public WritingResult {
        Objects.requireNonNull(outputText, "outputText");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(modelId, "modelId");
        Objects.requireNonNull(promptVersion, "promptVersion");
        if (latencyMs < 0) {
            throw new ValidationException("latencyMs must be >= 0");
        }
    }
}
