package com.adawriter.desktop;

import com.adawriter.writing.application.AssistWritingUseCase;
import com.adawriter.writing.domain.WritingAction;
import com.adawriter.writing.domain.WritingRequest;
import com.adawriter.writing.domain.WritingResult;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Clipboard-driven writing assistance for the desktop shell.
 */
public final class ClipboardAssistService {

    private final AssistWritingUseCase assistWriting;

    public ClipboardAssistService(AssistWritingUseCase assistWriting) {
        this.assistWriting = Objects.requireNonNull(assistWriting, "assistWriting");
    }

    public WritingResult assistFromClipboard(
            WritingAction action, Supplier<String> clipboardRead, Consumer<String> clipboardWrite) {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(clipboardRead, "clipboardRead");
        Objects.requireNonNull(clipboardWrite, "clipboardWrite");

        String text = clipboardRead.get();
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("Clipboard is empty");
        }
        WritingResult result = assistWriting.execute(WritingRequest.of(text, action));
        clipboardWrite.accept(result.outputText());
        return result;
    }
}
