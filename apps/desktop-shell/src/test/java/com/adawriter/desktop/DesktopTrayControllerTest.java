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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class DesktopTrayControllerTest {

    @Test
    void positive_startWhenUnsupportedReturnsFalse() {
        DesktopTrayController controller = new DesktopTrayController(
                clipboardAssist(), () -> {}, new FakeTrayGateway(false, false), memoryClipboard("hello"));

        assertThat(controller.start()).isFalse();
        controller.stop();
    }

    @Test
    void positive_startAndStopWithFakeTray() {
        FakeTrayGateway tray = new FakeTrayGateway(true, false);
        DesktopTrayController controller =
                new DesktopTrayController(clipboardAssist(), () -> {}, tray, memoryClipboard("hello world"));

        assertThat(controller.start()).isTrue();
        assertThat(tray.installed).isTrue();
        controller.stop();
        assertThat(tray.installed).isFalse();
    }

    @Test
    void positive_runAssistWritesClipboardAndShowsInfo() {
        FakeTrayGateway tray = new FakeTrayGateway(true, false);
        AtomicReference<String> clip = new AtomicReference<>("hello world");
        DesktopTrayController controller =
                new DesktopTrayController(clipboardAssist(), () -> {}, tray, new MemoryClipboard(clip));

        assertThat(controller.start()).isTrue();
        controller.runAssist(WritingAction.REWRITE);
        assertThat(clip.get()).isNotBlank();
        assertThat(tray.infoMessages).isNotEmpty();
        controller.stop();
    }

    @Test
    void negative_runAssistEmptyClipboardShowsError() {
        FakeTrayGateway tray = new FakeTrayGateway(true, false);
        DesktopTrayController controller =
                new DesktopTrayController(clipboardAssist(), () -> {}, tray, memoryClipboard("  "));

        assertThat(controller.start()).isTrue();
        assertThatCode(() -> controller.runAssist(WritingAction.SHORTEN)).doesNotThrowAnyException();
        assertThat(tray.errorMessages).isNotEmpty();
        controller.stop();
    }

    @Test
    void negative_startFailureReturnsFalse() {
        FakeTrayGateway tray = new FakeTrayGateway(true, true);
        DesktopTrayController controller =
                new DesktopTrayController(clipboardAssist(), () -> {}, tray, memoryClipboard("x"));

        assertThat(controller.start()).isFalse();
    }

    @Test
    void positive_exitCallbackWiredOnConstruction() {
        AtomicBoolean exited = new AtomicBoolean(false);
        DesktopTrayController controller = new DesktopTrayController(
                clipboardAssist(), () -> exited.set(true), new FakeTrayGateway(false, false), memoryClipboard(""));
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
        private final boolean failInstall;
        private boolean installed;
        private final List<String> infoMessages = new ArrayList<>();
        private final List<String> errorMessages = new ArrayList<>();

        private FakeTrayGateway(boolean supported, boolean failInstall) {
            this.supported = supported;
            this.failInstall = failInstall;
        }

        @Override
        public boolean isSupported() {
            return supported;
        }

        @Override
        public boolean install(Consumer<WritingAction> onAction, Runnable onExit) {
            if (failInstall) {
                return false;
            }
            installed = true;
            return true;
        }

        @Override
        public void uninstall() {
            installed = false;
        }

        @Override
        public void notifyInfo(String caption, String text) {
            infoMessages.add(caption + ":" + text);
        }

        @Override
        public void notifyError(String caption, String text) {
            errorMessages.add(caption + ":" + text);
        }
    }
}
