package com.adawriter.writing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class WritingRequestTest {

    @Test
    void rejectsBlankText() {
        assertThatThrownBy(() -> WritingRequest.of("  ", WritingAction.REWRITE))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsOversizedText() {
        String huge = "a".repeat(WritingConstraints.MAX_TEXT_CHARS + 1);
        assertThatThrownBy(() -> WritingRequest.of(huge, WritingAction.REWRITE))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("max length");
    }

    @Test
    void requiresToneForChangeTone() {
        assertThatThrownBy(() -> WritingRequest.builder("Hello", WritingAction.CHANGE_TONE)
                        .build())
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("tone");
    }

    @Test
    void buildsValidRequest() {
        WritingRequest request = WritingRequest.builder("Hello world", WritingAction.REWRITE)
                .tone(WritingTone.PROFESSIONAL)
                .locale("en-GB")
                .build();

        assertThat(request.text()).isEqualTo("Hello world");
        assertThat(request.action()).isEqualTo(WritingAction.REWRITE);
        assertThat(request.tone()).contains(WritingTone.PROFESSIONAL);
        assertThat(request.locale()).isEqualTo("en-GB");
    }
}
