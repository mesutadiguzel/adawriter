package com.adawriter.keyboard;

import android.inputmethodservice.InputMethodService;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import com.adawriter.writing.domain.WritingAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AdaWriter input method service with on-device assist actions.
 */
public final class AdaWriterImeService extends InputMethodService {

    private static final Logger log = LoggerFactory.getLogger(AdaWriterImeService.class);

    private final KeyboardAssistFacade assistFacade = KeyboardAssistFacade.onDeviceDefaults();

    @Override
    public View onCreateInputView() {
        View view = getLayoutInflater().inflate(R.layout.input_view, null);
        bind(view, R.id.btn_rewrite, WritingAction.REWRITE);
        bind(view, R.id.btn_shorten, WritingAction.SHORTEN);
        bind(view, R.id.btn_expand, WritingAction.EXPAND);
        bind(view, R.id.btn_fix, WritingAction.FIX_GRAMMAR);
        return view;
    }

    private void bind(View root, int buttonId, WritingAction action) {
        Button button = root.findViewById(buttonId);
        button.setOnClickListener(v -> applyAssist(action));
    }

    private void applyAssist(WritingAction action) {
        InputConnection connection = getCurrentInputConnection();
        if (connection == null) {
            return;
        }
        CharSequence before = connection.getTextBeforeCursor(10_000, 0);
        CharSequence after = connection.getTextAfterCursor(10_000, 0);
        String text = String.valueOf(before == null ? "" : before)
                + String.valueOf(after == null ? "" : after);
        if (text.isBlank()) {
            return;
        }
        try {
            String output = assistFacade.assist(text, action).outputText();
            connection.deleteSurroundingText(
                    before == null ? 0 : before.length(), after == null ? 0 : after.length());
            connection.commitText(output, 1);
        } catch (RuntimeException ex) {
            log.warn("keyboard_assist_failed action={} reason={}", action, ex.getMessage());
        }
    }
}
