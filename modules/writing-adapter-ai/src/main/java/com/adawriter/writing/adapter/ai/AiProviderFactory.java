package com.adawriter.writing.adapter.ai;

import com.adawriter.writing.domain.AiProviderPort;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

/**
 * Selects AI provider from environment for the composition root.
 *
 * <p>Env:
 * <ul>
 *   <li>{@code ADAWRITER_AI_PROVIDER}=offline|openai-compatible|openai-compatible+offline (default offline)</li>
 *   <li>{@code ADAWRITER_AI_BASE_URL} default https://api.openai.com/v1</li>
 *   <li>{@code ADAWRITER_AI_API_KEY}</li>
 *   <li>{@code ADAWRITER_AI_MODEL} default gpt-4o-mini</li>
 * </ul>
 */
public final class AiProviderFactory {

    private AiProviderFactory() {}

    public static AiProviderPort fromEnvironment() {
        String provider = env("ADAWRITER_AI_PROVIDER", "offline").trim().toLowerCase(Locale.ROOT);
        return switch (provider) {
            case "offline", "local", "stub" -> new OfflineRuleBasedAiProvider();
            case "openai-compatible", "openai" -> openAiCompatible();
            case "openai-compatible+offline", "openai+offline", "openai-compatible+stub", "openai+stub" ->
                new FallbackAiProvider(List.of(openAiCompatible(), new OfflineRuleBasedAiProvider()));
            default -> throw new IllegalArgumentException("Unknown ADAWRITER_AI_PROVIDER: " + provider);
        };
    }

    private static OpenAiCompatibleAiProvider openAiCompatible() {
        String base = env("ADAWRITER_AI_BASE_URL", "https://api.openai.com/v1").replaceAll("/$", "");
        String apiKey = env("ADAWRITER_AI_API_KEY", "");
        String model = env("ADAWRITER_AI_MODEL", "gpt-4o-mini");
        URI endpoint = URI.create(base + "/chat/completions");
        return new OpenAiCompatibleAiProvider(endpoint, apiKey, model, Duration.ofSeconds(60));
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
