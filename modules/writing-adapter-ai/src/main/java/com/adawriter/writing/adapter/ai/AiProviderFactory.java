package com.adawriter.writing.adapter.ai;

import com.adawriter.writing.domain.AiProviderPort;
import com.adawriter.writing.domain.RoutingPreference;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

/**
 * Selects AI provider from environment for the composition root.
 */
public final class AiProviderFactory {

    private AiProviderFactory() {}

    public static AiProviderPort fromEnvironment() {
        return from(System::getenv);
    }

    public static AiProviderPort from(Function<String, String> env) {
        Objects.requireNonNull(env, "env");
        String provider = value(env, "ADAWRITER_AI_PROVIDER", "offline").trim().toLowerCase(Locale.ROOT);
        return switch (provider) {
            case "offline", "local", "stub" -> withCircuit(new OfflineRuleBasedAiProvider());
            case "openai-compatible", "openai" -> withCircuit(openAiCompatible(env));
            case "openai-compatible+offline", "openai+offline", "openai-compatible+stub", "openai+stub" ->
                withCircuit(new FallbackAiProvider(
                        List.of(withCircuit(openAiCompatible(env)), withCircuit(new OfflineRuleBasedAiProvider()))));
            case "routed" -> routed(env);
            default -> throw new IllegalArgumentException("Unknown ADAWRITER_AI_PROVIDER: " + provider);
        };
    }

    private static AiProviderPort routed(Function<String, String> env) {
        RoutingPreference preference = parseRoute(env);
        List<RoutingAiProvider.RankedProvider> ranked = new ArrayList<>();
        ranked.add(new RoutingAiProvider.RankedProvider(
                withCircuit(openAiCompatible(env)), /*cost*/ 50, /*latency*/ 40, /*quality*/ 80));
        ranked.add(new RoutingAiProvider.RankedProvider(
                withCircuit(new OfflineRuleBasedAiProvider()), /*cost*/ 1, /*latency*/ 1, /*quality*/ 20));
        return new RoutingAiProvider(ranked, preference);
    }

    private static RoutingPreference parseRoute(Function<String, String> env) {
        String raw = value(env, "ADAWRITER_AI_ROUTE", "cost").trim().toUpperCase(Locale.ROOT);
        try {
            return RoutingPreference.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid ADAWRITER_AI_ROUTE: " + raw, ex);
        }
    }

    private static CircuitBreakingAiProvider withCircuit(AiProviderPort provider) {
        return new CircuitBreakingAiProvider(provider, 3, Duration.ofSeconds(30));
    }

    private static OpenAiCompatibleAiProvider openAiCompatible(Function<String, String> env) {
        String base =
                value(env, "ADAWRITER_AI_BASE_URL", "https://api.openai.com/v1").replaceAll("/$", "");
        String apiKey = value(env, "ADAWRITER_AI_API_KEY", "");
        String model = value(env, "ADAWRITER_AI_MODEL", "gpt-4o-mini");
        URI endpoint = URI.create(base + "/chat/completions");
        return new OpenAiCompatibleAiProvider(endpoint, apiKey, model, Duration.ofSeconds(60));
    }

    private static String value(Function<String, String> env, String key, String defaultValue) {
        String value = env.apply(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
