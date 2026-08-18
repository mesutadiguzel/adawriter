package com.adawriter.privacy.domain;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * Immutable detection result.
 */
public record DetectionResult(String originalText, List<SensitiveSpan> spans) implements Serializable {

    private static final long serialVersionUID = 1L;

    public DetectionResult {
        Objects.requireNonNull(originalText, "originalText");
        Objects.requireNonNull(spans, "spans");
        spans = List.copyOf(spans);
    }

    public boolean hasFindings() {
        return !spans.isEmpty();
    }

    public int findingCount() {
        return spans.size();
    }
}
