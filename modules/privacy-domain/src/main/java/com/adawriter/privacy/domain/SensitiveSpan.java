package com.adawriter.privacy.domain;

import java.io.Serializable;
import java.util.Objects;

/**
 * A sensitive span located in source text. Never carries the raw secret value.
 */
public record SensitiveSpan(int startInclusive, int endExclusive, SensitivityCategory category, String redactionToken)
        implements Serializable {

    private static final long serialVersionUID = 1L;

    public SensitiveSpan {
        if (startInclusive < 0 || endExclusive <= startInclusive) {
            throw new IllegalArgumentException("Invalid span bounds");
        }
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(redactionToken, "redactionToken");
        if (redactionToken.isBlank()) {
            throw new IllegalArgumentException("redactionToken must not be blank");
        }
    }

    public int length() {
        return endExclusive - startInclusive;
    }
}
