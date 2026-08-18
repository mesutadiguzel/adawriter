package com.adawriter.writing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class AiDomainTypesTest {

    @Test
    void positive_completionCommandAndResult() {
        AiCompletionCommand command = new AiCompletionCommand("sys", "user", 32);
        AiCompletionResult result = new AiCompletionResult("out", "model", 12L);
        assertThat(command.systemPrompt()).isEqualTo("sys");
        assertThat(result.latencyMs()).isEqualTo(12L);
    }

    @Test
    void negative_rejectsInvalidTokensAndLatency() {
        assertThatThrownBy(() -> new AiCompletionCommand("sys", "user", 0)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new AiCompletionResult("out", "model", -1)).isInstanceOf(ValidationException.class);
    }

    @Test
    void positive_providerAndUnexpectedExceptions() {
        AiProviderException provider = new AiProviderException("fail", new IllegalStateException("x"));
        UnexpectedWritingException unexpected =
                new UnexpectedWritingException("boom", new IllegalArgumentException("y"));
        assertThat(provider).isInstanceOf(WritingException.class).hasMessage("fail");
        assertThat(unexpected).isInstanceOf(WritingException.class).hasCauseInstanceOf(IllegalArgumentException.class);
        assertThat(new AiProviderException("solo")).hasMessage("solo");
    }

    @ParameterizedTest
    @EnumSource(RoutingPreference.class)
    void positive_routingPreferencesExist(RoutingPreference preference) {
        assertThat(preference.name()).isNotBlank();
    }
}
