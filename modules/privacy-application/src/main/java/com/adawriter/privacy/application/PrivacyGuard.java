package com.adawriter.privacy.application;

import com.adawriter.privacy.domain.DetectionResult;
import com.adawriter.privacy.domain.RedactionPolicy;
import com.adawriter.privacy.domain.RedactionResult;
import com.adawriter.privacy.domain.SensitiveTextDetector;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application facade for privacy detection and redaction.
 */
public final class PrivacyGuard {

    private static final Logger log = LoggerFactory.getLogger(PrivacyGuard.class);

    private final SensitiveTextDetector detector;
    private final RedactionPolicy defaultAssistPolicy;

    public PrivacyGuard(SensitiveTextDetector detector, RedactionPolicy defaultAssistPolicy) {
        this.detector = Objects.requireNonNull(detector, "detector");
        this.defaultAssistPolicy = Objects.requireNonNull(defaultAssistPolicy, "defaultAssistPolicy");
    }

    public static PrivacyGuard withDefaults() {
        return new PrivacyGuard(new SensitiveTextDetector(), RedactionPolicy.REDACT);
    }

    public DetectionResult detect(String text) {
        DetectionResult result = detector.detect(Objects.requireNonNull(text, "text"));
        if (result.hasFindings()) {
            log.info(
                    "privacy_detect findings={} categories={}",
                    result.findingCount(),
                    result.spans().stream()
                            .map(s -> s.category().name())
                            .distinct()
                            .toList());
        }
        return result;
    }

    public RedactionResult protectForAssist(String text) {
        return protect(text, defaultAssistPolicy);
    }

    public RedactionResult protect(String text, RedactionPolicy policy) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(policy, "policy");
        RedactionResult result = detector.apply(text, policy);
        if (result.detection().hasFindings()) {
            log.info(
                    "privacy_protect policy={} findings={} blocked={}",
                    policy,
                    result.detection().findingCount(),
                    result.blocked());
        }
        return result;
    }

    public RedactionPolicy defaultAssistPolicy() {
        return defaultAssistPolicy;
    }
}
