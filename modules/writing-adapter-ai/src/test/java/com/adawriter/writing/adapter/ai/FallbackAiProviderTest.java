package com.adawriter.writing.adapter.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.adawriter.writing.domain.AiCompletionCommand;
import com.adawriter.writing.domain.AiCompletionResult;
import com.adawriter.writing.domain.AiProviderException;
import com.adawriter.writing.domain.AiProviderPort;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class FallbackAiProviderTest {

    @Test
    void failsOverToSecondaryProvider() {
        AtomicInteger primaryCalls = new AtomicInteger();
        AiProviderPort primary = new AiProviderPort() {
            @Override
            public AiCompletionResult complete(AiCompletionCommand command) {
                primaryCalls.incrementAndGet();
                throw new AiProviderException("primary down");
            }

            @Override
            public String providerId() {
                return "primary";
            }
        };
        AiProviderPort secondary = new AiProviderPort() {
            @Override
            public AiCompletionResult complete(AiCompletionCommand command) {
                return new AiCompletionResult("ok", "secondary-model", 3L);
            }

            @Override
            public String providerId() {
                return "secondary";
            }
        };

        FallbackAiProvider fallback = new FallbackAiProvider(List.of(primary, secondary));
        AiCompletionResult result = fallback.complete(new AiCompletionCommand("sys", "user", 128));

        assertThat(primaryCalls.get()).isEqualTo(1);
        assertThat(result.text()).isEqualTo("ok");
        assertThat(fallback.providerId()).contains("primary").contains("secondary");
    }

    @Test
    void throwsWhenAllProvidersFail() {
        AiProviderPort failing = new AiProviderPort() {
            @Override
            public AiCompletionResult complete(AiCompletionCommand command) {
                throw new AiProviderException("down");
            }

            @Override
            public String providerId() {
                return "down";
            }
        };

        FallbackAiProvider fallback = new FallbackAiProvider(List.of(failing));
        assertThatThrownBy(() -> fallback.complete(new AiCompletionCommand("sys", "user", 128)))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("All AI providers failed");
    }
}
