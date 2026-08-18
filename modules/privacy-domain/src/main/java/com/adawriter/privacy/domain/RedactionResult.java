package com.adawriter.privacy.domain;

import java.util.Objects;

/**
 * Result of applying a redaction policy to text.
 */
public record RedactionResult(String text, DetectionResult detection, RedactionPolicy policyApplied, boolean blocked) {

    public RedactionResult {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(detection, "detection");
        Objects.requireNonNull(policyApplied, "policyApplied");
    }
}
