package com.adawriter.desktop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.adawriter.privacy.application.PrivacyGuard;
import com.adawriter.privacy.domain.RedactionPolicy;
import com.adawriter.privacy.domain.SensitiveTextDetector;
import com.adawriter.writing.adapter.ai.OfflineRuleBasedAiProvider;
import com.adawriter.writing.application.AssistWritingUseCase;
import com.adawriter.writing.application.WritingMetrics;
import com.adawriter.writing.domain.WritingAction;
import java.awt.AWTException;
import java.awt.Image;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class DesktopTrayControllerTest {

    @Test
    void positive_startWhenUnsupportedReturnsFalse() {
        DesktopTrayController controller =
                new DesktopTrayController(clipboardAssist(), () -> {}, unsupportedTray(), memoryClipboard("hello"));

        assertThat(controller.start()).isFalse();
        controller.stop();
    }

    @Test
    void positive_startAndStopWithFakeTray() throws Exception {
        FakeTrayGateway tray = new FakeTrayGateway(true);
        DesktopTrayController controller =
                new DesktopTrayController(clipboardAssist(), () -> {}, tray, memoryClipboard("hello world"));

        assertThat(controller.start()).isTrue();
        assertThat(tray.added).hasSize(1);
        controller.stop();
        assertThat(tray.removed).hasSize(1);
    }

    @Test
    void positive_runAssistWritesClipboardAndShowsInfo() {
        FakeTrayGateway tray = new FakeTrayGateway(true);
        AtomicReference<String> clip = new AtomicReference<>("hello world");
        DesktopTrayController controller =
                new DesktopTrayController(clipboardAssist(), () -> {}, tray, new MemoryClipboard(clip));

        assertThat(controller.start()).isTrue();
        controller.runAssist(WritingAction.REWRITE);
        assertThat(clip.get()).isNotBlank();
        assertThat(tray.messages).isNotEmpty();
        assertThat(tray.messages.get(0).type()).isEqualTo(TrayIcon.MessageType.INFO);
        controller.stop();
    }

    @Test
    void negative_runAssistEmptyClipboardShowsError() {
        FakeTrayGateway tray = new FakeTrayGateway(true);
        DesktopTrayController controller =
                new DesktopTrayController(clipboardAssist(), () -> {}, tray, memoryClipboard("  "));

        assertThat(controller.start()).isTrue();
        assertThatCode(() -> controller.runAssist(WritingAction.SHORTEN)).doesNotThrowAnyException();
        assertThat(tray.messages).isNotEmpty();
        assertThat(tray.messages.get(0).type()).isEqualTo(TrayIcon.MessageType.ERROR);
        controller.stop();
    }

    @Test
    void negative_startFailureReturnsFalse() {
        FakeTrayGateway tray = new FakeTrayGateway(true);
        tray.failAdd = true;
        DesktopTrayController controller =
                new DesktopTrayController(clipboardAssist(), () -> {}, tray, memoryClipboard("x"));

        assertThat(controller.start()).isFalse();
    }

    @Test
    void positive_exitCallbackWiredOnConstruction() {
        AtomicBoolean exited = new AtomicBoolean(false);
        DesktopTrayController controller = new DesktopTrayController(
                clipboardAssist(), () -> exited.set(true), unsupportedTray(), memoryClipboard(""));
        assertThat(controller.start()).isFalse();
        assertThat(exited).isFalse();
    }

    private static ClipboardAssistService clipboardAssist() {
        return new ClipboardAssistService(new AssistWritingUseCase(
                new OfflineRuleBasedAiProvider(),
                new WritingMetrics(),
                new PrivacyGuard(new SensitiveTextDetector(), RedactionPolicy.REDACT)));
    }

    private static ClipboardGateway memoryClipboard(String initial) {
        return new MemoryClipboard(new AtomicReference<>(initial));
    }

    private static SystemTrayGateway unsupportedTray() {
        return new FakeTrayGateway(false);
    }

    private record TrayMessage(String caption, String text, TrayIcon.MessageType type) {}

    private static final class MemoryClipboard implements ClipboardGateway {
        private final AtomicReference<String> value;

        private MemoryClipboard(AtomicReference<String> value) {
            this.value = value;
        }

        @Override
        public String readText() {
            return value.get();
        }

        @Override
        public void writeText(String text) {
            value.set(text);
        }
    }

    private static final class FakeTrayGateway implements SystemTrayGateway {
        private final boolean supported;
        private boolean failAdd;
        private final List<TrayIcon> added = new ArrayList<>();
        private final List<TrayIcon> removed = new ArrayList<>();
        private final List<TrayMessage> messages = new ArrayList<>();

        private FakeTrayGateway(boolean supported) {
            this.supported = supported;
        }

        @Override
        public boolean isSupported() {
            return supported;
        }

        @Override
        public Image createTrayImage() {
            return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        }

        @Override
        public void add(TrayIcon icon) throws AWTException {
            if (failAdd) {
                throw new AWTException("boom");
            }
            added.add(icon);
        }

        @Override
        public void remove(TrayIcon icon) {
            removed.add(icon);
        }

        @Override
        public void displayMessage(TrayIcon icon, String caption, String text, TrayIcon.MessageType type) {
            messages.add(new TrayMessage(caption, text, type));
        }
    }
}
