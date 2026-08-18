package com.adawriter.writing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class WritingResultTest {

    @Test
    void acceptsValidResult() {
        WritingResult result = new WritingResult("out", "offline", "model", "v1", 12L);
        assertThat(result.outputText()).isEqualTo("out");
        assertThat(result.latencyMs()).isEqualTo(12L);
    }

    @Test
    void rejectsNegativeLatency() {
        assertThatThrownBy(() -> new WritingResult("out", "p", "m", "v", -1L)).isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsNullFields() {
        assertThatThrownBy(() -> new WritingResult(null, "p", "m", "v", 0L)).isInstanceOf(NullPointerException.class);
    }
}
