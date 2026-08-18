package com.adawriter.keyboard;

import static org.assertj.core.api.Assertions.assertThat;

import com.adawriter.writing.domain.WritingAction;
import org.junit.jupiter.api.Test;

class KeyboardAssistFacadeTest {

    @Test
    void rewritesOnDevice() {
        KeyboardAssistFacade facade = KeyboardAssistFacade.onDeviceDefaults();
        assertThat(facade.assist("hello from adawriter", WritingAction.REWRITE).outputText())
                .isEqualTo("Hello from adawriter");
    }

    @Test
    void redactsEmailBeforeAssist() {
        KeyboardAssistFacade facade = KeyboardAssistFacade.onDeviceDefaults();
        String output = facade.assist("Contact jane.doe@example.com", WritingAction.REWRITE)
                .outputText();
        assertThat(output).doesNotContain("jane.doe@example.com");
    }
}
