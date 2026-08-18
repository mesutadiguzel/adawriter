package com.adawriter.privacy.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SensitiveSpanTest {

    @Test
    void positive_lengthAndFields() {
        SensitiveSpan span = new SensitiveSpan(1, 5, SensitivityCategory.EMAIL, "[EMAIL]");
        assertThat(span.length()).isEqualTo(4);
        assertThat(span.category()).isEqualTo(SensitivityCategory.EMAIL);
    }

    @Test
    void negative_rejectsInvalidBoundsAndBlankToken() {
        assertThatThrownBy(() -> new SensitiveSpan(-1, 2, SensitivityCategory.EMAIL, "[EMAIL]"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SensitiveSpan(2, 2, SensitivityCategory.EMAIL, "[EMAIL]"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SensitiveSpan(0, 2, SensitivityCategory.EMAIL, "  "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
