package com.adawriter.privacy.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SensitiveTextDetectorTest {

    private final SensitiveTextDetector detector = new SensitiveTextDetector();

    @Test
    void positive_detectsAndRedactsEmail() {
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
    void positive_detectsValidCreditCardWithLuhn() {
        String text = "Card 4111 1111 1111 1111 on file";
        DetectionResult detection = detector.detect(text);
        assertThat(detection.spans()).anyMatch(span -> span.category() == SensitivityCategory.CREDIT_CARD);
    }

    @Test
    void positive_detectsUsSsn() {
        DetectionResult detection = detector.detect("SSN 123-45-6789 on file");
        assertThat(detection.spans()).anyMatch(span -> span.category() == SensitivityCategory.US_SSN);
    }

    @Test
    void positive_detectsIpv4() {
        DetectionResult detection = detector.detect("Server at 192.168.1.10 replied");
        assertThat(detection.spans()).anyMatch(span -> span.category() == SensitivityCategory.IPV4);
    }

    @Test
    void positive_detectsPrivateKeyBlock() {
        String text =
                """
                -----BEGIN PRIVATE KEY-----
                MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC7
                -----END PRIVATE KEY-----
                """;
        DetectionResult detection = detector.detect(text);
        assertThat(detection.spans()).anyMatch(span -> span.category() == SensitivityCategory.PRIVATE_KEY_BLOCK);
    }

    @Test
    void positive_reportOnlyDoesNotAlterText() {
        String text = "mail me at a@b.co";
        RedactionResult result = detector.apply(text, RedactionPolicy.REPORT_ONLY);
        assertThat(result.text()).isEqualTo(text);
        assertThat(result.detection().hasFindings()).isTrue();
        assertThat(result.blocked()).isFalse();
    }

    @Test
    void negative_cleanTextHasNoFindings() {
        DetectionResult detection = detector.detect("AdaWriter helps writers stay private and fast.");
        assertThat(detection.hasFindings()).isFalse();
        assertThat(detector.apply(detection.originalText(), RedactionPolicy.BLOCK)
                        .text())
                .isEqualTo(detection.originalText());
    }

    @Test
    void negative_rejectsInvalidCardLikeDigits() {
        String text = "Order id 1234 5678 9012 3456 is not a card";
        DetectionResult detection = detector.detect(text);
        assertThat(detection.spans()).noneMatch(span -> span.category() == SensitivityCategory.CREDIT_CARD);
    }

    @Test
    void negative_blockPolicyThrowsOnSecret() {
        String text = "key=api_testkey_abcdefghijklmnopqr";
        assertThatThrownBy(() -> detector.apply(text, RedactionPolicy.BLOCK))
                .isInstanceOf(SensitiveContentBlockedException.class)
                .hasMessageContaining("blocked");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "plain note", "Call the office tomorrow"})
    void negative_emptyOrBenignTextIsSafe(String text) {
        assertThat(detector.detect(text).hasFindings()).isFalse();
    }

    @Test
    void negative_nullTextTreatedAsEmpty() {
        DetectionResult detection = detector.detect(null);
        assertThat(detection.originalText()).isEmpty();
        assertThat(detection.hasFindings()).isFalse();
    }

    @Test
    void positive_redactWithNoFindingsReturnsOriginal() {
        String text = "nothing sensitive";
        assertThat(detector.apply(text, RedactionPolicy.REDACT).text()).isEqualTo(text);
    }

    @Test
    void positive_resolvesOverlappingPrivateKeyAndEmailPreferringLongerSpan() {
        String text =
                """
                -----BEGIN PRIVATE KEY-----
                contact admin@example.com inside
                -----END PRIVATE KEY-----
                """;
        DetectionResult detection = detector.detect(text);
        assertThat(detection.spans()).isNotEmpty();
        assertThat(detection.spans()).anyMatch(span -> span.category() == SensitivityCategory.PRIVATE_KEY_BLOCK);
        RedactionResult redacted = detector.apply(text, RedactionPolicy.REDACT);
        assertThat(redacted.text()).contains("[PRIVATE_KEY_BLOCK]");
        assertThat(redacted.text()).doesNotContain("admin@example.com");
    }

    @Test
    void negative_blockExposesDetectionAccessor() {
        String text = "key=api_testkey_abcdefghijklmnopqr";
        try {
            detector.apply(text, RedactionPolicy.BLOCK);
        } catch (SensitiveContentBlockedException ex) {
            assertThat(ex.detection().hasFindings()).isTrue();
            return;
        }
        throw new AssertionError("expected SensitiveContentBlockedException");
    }
}
