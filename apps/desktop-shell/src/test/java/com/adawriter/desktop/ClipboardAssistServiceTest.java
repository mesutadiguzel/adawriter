package com.adawriter.desktop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.adawriter.privacy.application.PrivacyGuard;
import com.adawriter.writing.adapter.ai.OfflineRuleBasedAiProvider;
import com.adawriter.writing.application.AssistWritingUseCase;
import com.adawriter.writing.application.WritingMetrics;
import com.adawriter.writing.domain.WritingAction;
import com.adawriter.writing.domain.WritingResult;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ClipboardAssistServiceTest {

    @Test
    void rewritesClipboardText() {
        AssistWritingUseCase useCase = new AssistWritingUseCase(
                new OfflineRuleBasedAiProvider(), new WritingMetrics(), PrivacyGuard.withDefaults());
        ClipboardAssistService service = new ClipboardAssistService(useCase);
        AtomicReference<String> clipboard = new AtomicReference<>("hello from adawriter");

        WritingResult result = service.assistFromClipboard(WritingAction.REWRITE, clipboard::get, clipboard::set);

        assertThat(result.outputText()).isEqualTo("Hello from adawriter");
        assertThat(clipboard.get()).isEqualTo("Hello from adawriter");
    }

    @Test
    void rejectsEmptyClipboard() {
        AssistWritingUseCase useCase = new AssistWritingUseCase(
                new OfflineRuleBasedAiProvider(), new WritingMetrics(), PrivacyGuard.withDefaults());
        ClipboardAssistService service = new ClipboardAssistService(useCase);

        assertThatThrownBy(() -> service.assistFromClipboard(WritingAction.REWRITE, () -> "  ", t -> {}))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("empty");
    }
}
