package com.adawriter.writing.adapter.ai;

import com.adawriter.writing.domain.AiProviderPort;
import com.adawriter.writing.domain.RoutingPreference;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Selects AI provider from environment for the composition root.
 *
 * <p>Env:
 * <ul>
 *   <li>{@code ADAWRITER_AI_PROVIDER}=offline|openai-compatible|openai-compatible+offline|routed (default offline)</li>
 *   <li>{@code ADAWRITER_AI_ROUTE}=cost|latency|quality (default cost)</li>
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
            case "offline", "local", "stub" -> withCircuit(new OfflineRuleBasedAiProvider());
            case "openai-compatible", "openai" -> withCircuit(openAiCompatible());
            case "openai-compatible+offline", "openai+offline", "openai-compatible+stub", "openai+stub" ->
                withCircuit(new FallbackAiProvider(
                        List.of(withCircuit(openAiCompatible()), withCircuit(new OfflineRuleBasedAiProvider()))));
            case "routed" -> routed();
            default -> throw new IllegalArgumentException("Unknown ADAWRITER_AI_PROVIDER: " + provider);
        };
    }

    private static AiProviderPort routed() {
        RoutingPreference preference = parseRoute();
        List<RoutingAiProvider.RankedProvider> ranked = new ArrayList<>();
        ranked.add(new RoutingAiProvider.RankedProvider(
                withCircuit(openAiCompatible()), /*cost*/ 50, /*latency*/ 40, /*quality*/ 80));
        ranked.add(new RoutingAiProvider.RankedProvider(
                withCircuit(new OfflineRuleBasedAiProvider()), /*cost*/ 1, /*latency*/ 1, /*quality*/ 20));
        return new RoutingAiProvider(ranked, preference);
    }

    private static RoutingPreference parseRoute() {
        String raw = env("ADAWRITER_AI_ROUTE", "cost").trim().toUpperCase(Locale.ROOT);
        try {
            return RoutingPreference.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid ADAWRITER_AI_ROUTE: " + raw, ex);
        }
    }

    private static CircuitBreakingAiProvider withCircuit(AiProviderPort provider) {
        return new CircuitBreakingAiProvider(provider, 3, Duration.ofSeconds(30));
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
