package com.adawriter.writing.domain;

/**
 * Invalid client or domain input.
 */
public final class ValidationException extends WritingException {

    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }
}
