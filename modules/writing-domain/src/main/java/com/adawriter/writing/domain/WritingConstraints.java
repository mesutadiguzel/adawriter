package com.adawriter.writing.domain;

/**
 * Shared domain constraints for writing assistance.
 */
public final class WritingConstraints {

    public static final int MAX_TEXT_CHARS = 50_000;
    public static final int MAX_OUTPUT_CHARS = 100_000;
    public static final int DEFAULT_MAX_TOKENS = 2048;

    private WritingConstraints() {}
}
