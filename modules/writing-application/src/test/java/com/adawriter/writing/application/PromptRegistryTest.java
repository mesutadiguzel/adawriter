package com.adawriter.writing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.adawriter.writing.domain.WritingAction;
import com.adawriter.writing.domain.WritingRequest;
import com.adawriter.writing.domain.WritingTone;
import org.junit.jupiter.api.Test;

class PromptRegistryTest {

    @Test
    void positive_activeSystemPromptAvailable() {
        assertThat(PromptRegistry.activeVersion()).isEqualTo("writing-assist-v2");
        assertThat(PromptRegistry.systemPrompt()).contains("AdaWriter");
        assertThat(PromptRegistry.systemPrompt("writing-assist-v1")).contains("privacy-first");
    }

    @Test
    void positive_userPromptIncludesActionAndTone() {
        WritingRequest request = WritingRequest.builder("Body", WritingAction.CHANGE_TONE)
                .tone(WritingTone.PROFESSIONAL)
                .build();
        String prompt = PromptRegistry.userPrompt(request);
        assertThat(prompt).contains("CHANGE_TONE").contains("professional").contains("Body");
    }

    @Test
    void negative_unknownVersion() {
        assertThatThrownBy(() -> PromptRegistry.systemPrompt("nope")).isInstanceOf(IllegalArgumentException.class);
    }
}
