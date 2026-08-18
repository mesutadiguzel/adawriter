package com.adawriter.writing.domain;

/**
 * Unexpected failure inside the writing assistance pipeline.
 */
public final class UnexpectedWritingException extends WritingException {

    private static final long serialVersionUID = 1L;

    public UnexpectedWritingException(String message, Throwable cause) {
        super(message, cause);
    }
}
