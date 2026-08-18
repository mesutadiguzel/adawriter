package com.adawriter.privacy.domain;

/**
 * Raised when policy is BLOCK and sensitive content is present.
 */
public final class SensitiveContentBlockedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final DetectionResult detection;

    public SensitiveContentBlockedException(DetectionResult detection) {
        super("Sensitive content blocked by privacy policy (" + detection.findingCount() + " finding(s))");
        this.detection = detection;
    }

    public DetectionResult detection() {
        return detection;
    }
}
