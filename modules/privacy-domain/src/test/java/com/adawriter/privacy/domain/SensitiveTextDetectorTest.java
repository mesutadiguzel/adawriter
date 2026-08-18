package com.adawriter.privacy.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SensitiveTextDetectorTest {

    private final SensitiveTextDetector detector = new SensitiveTextDetector();

    @Test
    void detectsEmailAndRedacts() {
        String text = "Contact me at jane.doe@example.com please.";
        DetectionResult detection = detector.detect(text);
        assertThat(detection.hasFindings()).isTrue();
        assertThat(detection.spans()).singleElement().satisfies(span -> {
            assertThat(span.category()).isEqualTo(SensitivityCategory.EMAIL);
            assertThat(span.redactionToken()).isEqualTo("[EMAIL]");
        });

        RedactionResult redacted = detector.apply(text, RedactionPolicy.REDACT);
        assertThat(redacted.text()).isEqualTo("Contact me at [EMAIL] please.");
        assertThat(redacted.text()).doesNotContain("jane.doe@example.com");
    }

    @Test
    void detectsValidCreditCardWithLuhn() {
        // Visa test PAN that passes Luhn
        String text = "Card 4111 1111 1111 1111 on file";
        DetectionResult detection = detector.detect(text);
        assertThat(detection.spans()).anyMatch(span -> span.category() == SensitivityCategory.CREDIT_CARD);
    }

    @Test
    void rejectsInvalidCardLikeDigits() {
        String text = "Order id 1234 5678 9012 3456 is not a card";
        DetectionResult detection = detector.detect(text);
        assertThat(detection.spans()).noneMatch(span -> span.category() == SensitivityCategory.CREDIT_CARD);
    }

    @Test
    void blockPolicyThrows() {
        String text = "key=api_testkey_abcdefghijklmnopqr";
        assertThatThrownBy(() -> detector.apply(text, RedactionPolicy.BLOCK))
                .isInstanceOf(SensitiveContentBlockedException.class);
    }

    @Test
    void detectsPrivateKeyBlock() {
        String text =
                """
                -----BEGIN PRIVATE KEY-----
                MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC7
                -----END PRIVATE KEY-----
                """;
        DetectionResult detection = detector.detect(text);
        assertThat(detection.spans()).anyMatch(span -> span.category() == SensitivityCategory.PRIVATE_KEY_BLOCK);
    }
}
