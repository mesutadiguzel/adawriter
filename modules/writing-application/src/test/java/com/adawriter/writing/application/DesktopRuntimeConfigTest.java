package com.adawriter.writing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.adawriter.privacy.domain.RedactionPolicy;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DesktopRuntimeConfigTest {

    @Test
    void positive_defaults() {
        DesktopRuntimeConfig config = DesktopRuntimeConfig.from(key -> null);
        assertThat(config.port()).isEqualTo(8787);
        assertThat(config.assistPrivacyPolicy()).isEqualTo(RedactionPolicy.REDACT);
    }

    @Test
    void positive_readsEnvOverrides() {
        Map<String, String> env = Map.of("ADAWRITER_PORT", "9090", "ADAWRITER_PRIVACY_POLICY", "BLOCK");
        DesktopRuntimeConfig config = DesktopRuntimeConfig.from(env::get);
        assertThat(config.port()).isEqualTo(9090);
        assertThat(config.assistPrivacyPolicy()).isEqualTo(RedactionPolicy.BLOCK);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "65536", "-1", "abc"})
    void negative_rejectsInvalidPort(String raw) {
        assertThatThrownBy(() -> DesktopRuntimeConfig.parsePort(raw)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void negative_rejectsReportOnlyPolicy() {
        assertThatThrownBy(() -> DesktopRuntimeConfig.parseAssistPolicy("REPORT_ONLY"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("REDACT or BLOCK");
    }

    @Test
    void negative_rejectsUnknownPolicy() {
        assertThatThrownBy(() -> DesktopRuntimeConfig.parseAssistPolicy("YEET"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void negative_constructorRejectsInvalidValues() {
        assertThatThrownBy(() -> new DesktopRuntimeConfig(0, RedactionPolicy.REDACT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("port");
        assertThatThrownBy(() -> new DesktopRuntimeConfig(8787, RedactionPolicy.REPORT_ONLY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("REDACT or BLOCK");
    }
}
