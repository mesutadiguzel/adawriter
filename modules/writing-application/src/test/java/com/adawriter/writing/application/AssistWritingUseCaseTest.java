package com.adawriter.writing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.adawriter.privacy.application.PrivacyGuard;
import com.adawriter.writing.domain.AiCompletionCommand;
import com.adawriter.writing.domain.AiCompletionResult;
import com.adawriter.writing.domain.AiProviderPort;
import com.adawriter.writing.domain.ValidationException;
import com.adawriter.writing.domain.WritingAction;
import com.adawriter.writing.domain.WritingRequest;
import com.adawriter.writing.domain.WritingResult;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class AssistWritingUseCaseTest {

    @Test
    void returnsValidatedResultFromProvider() {
        AiProviderPort provider = new AiProviderPort() {
            @Override
            public AiCompletionResult complete(AiCompletionCommand command) {
                assertThat(command.systemPrompt()).contains("AdaWriter");
                assertThat(command.userPrompt()).contains("Hello");
                return new AiCompletionResult("```\nClean output\n```", "test-model", 12L);
            }

            @Override
            public String providerId() {
                return "test";
            }
        };

        WritingMetrics metrics = new WritingMetrics();
        AssistWritingUseCase useCase = new AssistWritingUseCase(provider, metrics, PrivacyGuard.withDefaults());

        WritingResult result = useCase.execute(WritingRequest.of("Hello", WritingAction.REWRITE));

        assertThat(result.outputText()).isEqualTo("Clean output");
        assertThat(result.providerId()).isEqualTo("test");
        assertThat(result.modelId()).isEqualTo("test-model");
        assertThat(result.promptVersion()).isEqualTo(PromptRegistry.activeVersion());
        assertThat(metrics.assistRequests()).isEqualTo(1);
        assertThat(metrics.assistFailures()).isZero();
    }

    @Test
    void redactsSensitiveContentBeforeProviderCall() {
        AtomicReference<String> seenUserPrompt = new AtomicReference<>();
        AiProviderPort provider = new AiProviderPort() {
            @Override
            public AiCompletionResult complete(AiCompletionCommand command) {
                seenUserPrompt.set(command.userPrompt());
                return new AiCompletionResult("ok", "test-model", 1L);
            }

            @Override
            public String providerId() {
                return "test";
            }
        };

        AssistWritingUseCase useCase =
                new AssistWritingUseCase(provider, new WritingMetrics(), PrivacyGuard.withDefaults());
        useCase.execute(WritingRequest.of("Email jane.doe@example.com now", WritingAction.REWRITE));

        assertThat(seenUserPrompt.get()).contains("[EMAIL]");
        assertThat(seenUserPrompt.get()).doesNotContain("jane.doe@example.com");
    }

    @Test
    void recordsFailureWhenProviderReturnsEmpty() {
        AiProviderPort provider = new AiProviderPort() {
            @Override
            public AiCompletionResult complete(AiCompletionCommand command) {
                return new AiCompletionResult("   ", "test-model", 1L);
            }

            @Override
            public String providerId() {
                return "test";
            }
        };

        WritingMetrics metrics = new WritingMetrics();
        AssistWritingUseCase useCase = new AssistWritingUseCase(provider, metrics, PrivacyGuard.withDefaults());

        assertThatThrownBy(() -> useCase.execute(WritingRequest.of("Hello", WritingAction.REWRITE)))
                .isInstanceOf(ValidationException.class);

        assertThat(metrics.assistFailures()).isEqualTo(1);
    }
}
