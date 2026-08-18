package com.adawriter.writing.domain;

import java.util.Objects;

/**
 * Provider-agnostic completion command.
 */
public record AiCompletionCommand(String systemPrompt, String userPrompt, int maxTokens) {

    public AiCompletionCommand {
        Objects.requireNonNull(systemPrompt, "systemPrompt");
        Objects.requireNonNull(userPrompt, "userPrompt");
        if (maxTokens <= 0) {
            throw new ValidationException("maxTokens must be > 0");
        }
    }
}
