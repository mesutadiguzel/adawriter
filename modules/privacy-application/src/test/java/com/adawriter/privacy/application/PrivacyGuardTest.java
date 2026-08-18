package com.adawriter.privacy.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.adawriter.privacy.domain.RedactionPolicy;
import com.adawriter.privacy.domain.SensitiveContentBlockedException;
import com.adawriter.privacy.domain.SensitiveTextDetector;
import org.junit.jupiter.api.Test;

class PrivacyGuardTest {

    @Test
    void positive_defaultsUseRedact() {
        PrivacyGuard guard = PrivacyGuard.withDefaults();
        assertThat(guard.defaultAssistPolicy()).isEqualTo(RedactionPolicy.REDACT);
        assertThat(guard.protectForAssist("mail a@b.co").text()).contains("[EMAIL]");
    }

    @Test
    void positive_detectReportsFindings() {
        PrivacyGuard guard = PrivacyGuard.withDefaults();
        assertThat(guard.detect("x@y.zz").hasFindings()).isTrue();
        assertThat(guard.detect("clean text").hasFindings()).isFalse();
    }

    @Test
    void negative_blockPolicyThrows() {
        PrivacyGuard guard = new PrivacyGuard(new SensitiveTextDetector(), RedactionPolicy.BLOCK);
        assertThatThrownBy(() -> guard.protectForAssist("api_testkey_abcdefghijklmnopqr"))
                .isInstanceOf(SensitiveContentBlockedException.class);
    }

    @Test
    void negative_nullTextRejected() {
        assertThatThrownBy(() -> PrivacyGuard.withDefaults().detect(null)).isInstanceOf(NullPointerException.class);
    }
}
