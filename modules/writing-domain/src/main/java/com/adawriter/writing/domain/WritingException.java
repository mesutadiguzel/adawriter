package com.adawriter.writing.domain;

/**
 * Base type for domain and application failures that are safe to classify at adapters.
 */
public abstract class WritingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected WritingException(String message) {
        super(message);
    }

    protected WritingException(String message, Throwable cause) {
        super(message, cause);
    }
}
