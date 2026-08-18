package com.adawriter.keyboard;

import com.adawriter.privacy.application.PrivacyGuard;
import com.adawriter.writing.adapter.ai.OfflineRuleBasedAiProvider;
import com.adawriter.writing.application.AssistWritingUseCase;
import com.adawriter.writing.application.WritingMetrics;
import com.adawriter.writing.domain.WritingAction;
import com.adawriter.writing.domain.WritingRequest;
import com.adawriter.writing.domain.WritingResult;
import java.util.Objects;

/**
 * On-device keyboard assist facade using the shared hexagonal core.
 */
public final class KeyboardAssistFacade {

    private final AssistWritingUseCase assistWriting;

    public KeyboardAssistFacade(AssistWritingUseCase assistWriting) {
        this.assistWriting = Objects.requireNonNull(assistWriting, "assistWriting");
    }

    public static KeyboardAssistFacade onDeviceDefaults() {
        return new KeyboardAssistFacade(new AssistWritingUseCase(
                new OfflineRuleBasedAiProvider(), new WritingMetrics(), PrivacyGuard.withDefaults()));
    }

    public WritingResult assist(String text, WritingAction action) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(action, "action");
        return assistWriting.execute(WritingRequest.of(text, action));
    }
}
