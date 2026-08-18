package com.adawriter.desktop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.adawriter.privacy.application.PrivacyGuard;
import com.adawriter.privacy.domain.RedactionPolicy;
import com.adawriter.privacy.domain.SensitiveContentBlockedException;
import com.adawriter.privacy.domain.SensitiveTextDetector;
import com.adawriter.writing.adapter.ai.OfflineRuleBasedAiProvider;
import com.adawriter.writing.application.AssistWritingUseCase;
import com.adawriter.writing.application.WritingMetrics;
import com.adawriter.writing.domain.WritingAction;
import com.adawriter.writing.domain.WritingResult;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ClipboardAssistServiceTest {

    private ClipboardAssistService service;
    private AtomicReference<String> clipboard;

    @BeforeEach
    void setUp() {
        AssistWritingUseCase useCase = new AssistWritingUseCase(
                new OfflineRuleBasedAiProvider(), new WritingMetrics(), PrivacyGuard.withDefaults());
        service = new ClipboardAssistService(useCase);
        clipboard = new AtomicReference<>();
    }

    @Test
    void positive_rewritesClipboardText() {
        clipboard.set("hello from adawriter");
        WritingResult result = service.assistFromClipboard(WritingAction.REWRITE, clipboard::get, clipboard::set);
        assertThat(result.outputText()).isEqualTo("Hello from adawriter");
        assertThat(clipboard.get()).isEqualTo("Hello from adawriter");
    }

    @ParameterizedTest
    @EnumSource(
            value = WritingAction.class,
            names = {"REWRITE", "SHORTEN", "EXPAND", "FIX_GRAMMAR"})
    void positive_supportsClipboardActions(WritingAction action) {
        clipboard.set("AdaWriter keeps drafts private. Second sentence stays.");
        WritingResult result = service.assistFromClipboard(action, clipboard::get, clipboard::set);
        assertThat(result.outputText()).isNotBlank();
        assertThat(clipboard.get()).isEqualTo(result.outputText());
    }

    @Test
    void positive_redactsEmailInClipboardBeforeAssist() {
        clipboard.set("Email jane.doe@example.com please");
        WritingResult result = service.assistFromClipboard(WritingAction.REWRITE, clipboard::get, clipboard::set);
        assertThat(result.outputText()).doesNotContain("jane.doe@example.com");
        assertThat(clipboard.get()).doesNotContain("jane.doe@example.com");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void negative_rejectsEmptyClipboard(String value) {
        clipboard.set(value);
        assertThatThrownBy(() -> service.assistFromClipboard(WritingAction.REWRITE, clipboard::get, clipboard::set))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void negative_rejectsNullAction() {
        clipboard.set("hello");
        assertThatThrownBy(() -> service.assistFromClipboard(null, clipboard::get, clipboard::set))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void negative_blockPolicyPreventsClipboardWrite() {
        AssistWritingUseCase blockingUseCase = new AssistWritingUseCase(
                new OfflineRuleBasedAiProvider(),
                new WritingMetrics(),
                new PrivacyGuard(new SensitiveTextDetector(), RedactionPolicy.BLOCK));
        ClipboardAssistService blockingService = new ClipboardAssistService(blockingUseCase);
        clipboard.set("key=api_testkey_abcdefghijklmnopqr");

        assertThatThrownBy(() ->
                        blockingService.assistFromClipboard(WritingAction.REWRITE, clipboard::get, clipboard::set))
                .isInstanceOf(SensitiveContentBlockedException.class);
        assertThat(clipboard.get()).isEqualTo("key=api_testkey_abcdefghijklmnopqr");
    }
}
