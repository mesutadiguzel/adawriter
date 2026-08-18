package com.adawriter.writing.domain;

/**
 * AI provider port failure (network, protocol, empty response, etc.).
 */
public final class AiProviderException extends WritingException {

    private static final long serialVersionUID = 1L;

    public AiProviderException(String message) {
        super(message);
    }

    public AiProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
