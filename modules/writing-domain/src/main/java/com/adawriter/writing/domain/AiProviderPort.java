package com.adawriter.writing.domain;

/**
 * Outbound port for AI completions used by writing assistance.
 */
public interface AiProviderPort {

    AiCompletionResult complete(AiCompletionCommand command);

    String providerId();
}
