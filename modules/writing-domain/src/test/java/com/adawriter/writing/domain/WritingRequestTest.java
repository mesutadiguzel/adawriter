package com.adawriter.writing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class WritingRequestTest {

    @Test
    void buildsValidRewriteRequest() {
        WritingRequest request = WritingRequest.builder("Hello world", WritingAction.REWRITE)
                .tone(WritingTone.PROFESSIONAL)
                .locale("en-GB")
                .build();

        assertThat(request.text()).isEqualTo("Hello world");
        assertThat(request.action()).isEqualTo(WritingAction.REWRITE);
        assertThat(request.tone()).contains(WritingTone.PROFESSIONAL);
        assertThat(request.locale()).isEqualTo("en-GB");
    }

    @ParameterizedTest
    @EnumSource(
            value = WritingAction.class,
            names = {"REWRITE", "SHORTEN", "EXPAND", "FIX_GRAMMAR"})
    void acceptsActionsWithoutTone(WritingAction action) {
        WritingRequest request = WritingRequest.of("Sample text", action);
        assertThat(request.action()).isEqualTo(action);
        assertThat(request.tone()).isEmpty();
    }

    @Test
    void acceptsChangeToneWhenToneProvided() {
        WritingRequest request = WritingRequest.builder("Hello", WritingAction.CHANGE_TONE)
                .tone(WritingTone.CASUAL)
                .build();
        assertThat(request.tone()).contains(WritingTone.CASUAL);
    }

    @Test
    void withTextPreservesActionToneAndLocale() {
        WritingRequest original = WritingRequest.builder("old", WritingAction.REWRITE)
                .tone(WritingTone.FRIENDLY)
                .locale("en-US")
                .build();
        WritingRequest updated = original.withText("new text");
        assertThat(updated.text()).isEqualTo("new text");
        assertThat(updated.action()).isEqualTo(WritingAction.REWRITE);
        assertThat(updated.tone()).contains(WritingTone.FRIENDLY);
        assertThat(updated.locale()).isEqualTo("en-US");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void rejectsBlankOrNullText(String text) {
        assertThatThrownBy(() -> WritingRequest.of(text, WritingAction.REWRITE))
                .isInstanceOfAny(ValidationException.class, NullPointerException.class);
    }

    @Test
    void rejectsOversizedText() {
        String huge = "a".repeat(WritingConstraints.MAX_TEXT_CHARS + 1);
        assertThatThrownBy(() -> WritingRequest.of(huge, WritingAction.REWRITE))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("max length");
    }

    @Test
    void acceptsTextAtMaxLength() {
        String max = "a".repeat(WritingConstraints.MAX_TEXT_CHARS);
        WritingRequest request = WritingRequest.of(max, WritingAction.REWRITE);
        assertThat(request.text()).hasSize(WritingConstraints.MAX_TEXT_CHARS);
    }

    @Test
    void requiresToneForChangeTone() {
        assertThatThrownBy(() -> WritingRequest.builder("Hello", WritingAction.CHANGE_TONE)
                        .build())
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("tone");
    }

    @Test
    void rejectsBlankLocale() {
        assertThatThrownBy(() -> WritingRequest.builder("Hello", WritingAction.REWRITE)
                        .locale("  ")
                        .build())
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsNullAction() {
        assertThatThrownBy(() -> WritingRequest.of("Hello", null)).isInstanceOf(NullPointerException.class);
    }
}
