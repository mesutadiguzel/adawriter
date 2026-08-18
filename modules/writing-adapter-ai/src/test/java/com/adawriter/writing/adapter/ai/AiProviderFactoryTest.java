package com.adawriter.writing.adapter.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.adawriter.writing.domain.AiProviderPort;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AiProviderFactoryTest {

    @Test
    void positive_defaultsToOfflineCircuit() {
        AiProviderPort provider = AiProviderFactory.from(key -> null);
        assertThat(provider.providerId()).isEqualTo(OfflineRuleBasedAiProvider.PROVIDER_ID);
        assertThat(provider).isInstanceOf(CircuitBreakingAiProvider.class);
    }

    @Test
    void positive_routedProviderIdIncludesPreference() {
        AiProviderPort provider = AiProviderFactory.from(Map.of(
                "ADAWRITER_AI_PROVIDER", "routed",
                "ADAWRITER_AI_ROUTE", "latency")::get);
        assertThat(provider.providerId()).contains("latency");
    }

    @Test
    void positive_openaiCompatibleAndFallbackModes() {
        AiProviderPort openai = AiProviderFactory.from(Map.of(
                "ADAWRITER_AI_PROVIDER", "openai-compatible",
                "ADAWRITER_AI_BASE_URL", "http://127.0.0.1:9/v1",
                "ADAWRITER_AI_API_KEY", "api_testkey_abcdefghijklmnopqr")::get);
        assertThat(openai.providerId()).isEqualTo(OpenAiCompatibleAiProvider.PROVIDER_ID);

        AiProviderPort fallback = AiProviderFactory.from(Map.of(
                "ADAWRITER_AI_PROVIDER", "openai+offline",
                "ADAWRITER_AI_BASE_URL", "http://127.0.0.1:9/v1")::get);
        assertThat(fallback.providerId()).contains(OpenAiCompatibleAiProvider.PROVIDER_ID);
    }

    @Test
    void negative_unknownProvider() {
        assertThatThrownBy(() -> AiProviderFactory.from(Map.of("ADAWRITER_AI_PROVIDER", "magic")::get))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown");
    }

    @Test
    void negative_invalidRoute() {
        assertThatThrownBy(() -> AiProviderFactory.from(Map.of(
                        "ADAWRITER_AI_PROVIDER", "routed",
                        "ADAWRITER_AI_ROUTE", "banana")::get))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ADAWRITER_AI_ROUTE");
    }
}
