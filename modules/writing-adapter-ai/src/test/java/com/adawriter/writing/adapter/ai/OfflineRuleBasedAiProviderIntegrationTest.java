package com.adawriter.writing.adapter.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.adawriter.privacy.application.PrivacyGuard;
import com.adawriter.writing.application.AssistWritingUseCase;
import com.adawriter.writing.application.WritingMetrics;
import com.adawriter.writing.domain.WritingAction;
import com.adawriter.writing.domain.WritingRequest;
import com.adawriter.writing.domain.WritingResult;
import org.junit.jupiter.api.Test;

class OfflineRuleBasedAiProviderIntegrationTest {

    @Test
    void assistRewriteUsesOfflineProvider() {
        AssistWritingUseCase useCase = new AssistWritingUseCase(
                new OfflineRuleBasedAiProvider(), new WritingMetrics(), PrivacyGuard.withDefaults());

        WritingResult result =
                useCase.execute(WritingRequest.of("adaWriter helps writers stay private.", WritingAction.REWRITE));

        assertThat(result.providerId()).isEqualTo(OfflineRuleBasedAiProvider.PROVIDER_ID);
        assertThat(result.modelId()).isEqualTo(OfflineRuleBasedAiProvider.MODEL_ID);
        assertThat(result.outputText()).isEqualTo("AdaWriter helps writers stay private.");
    }
}
