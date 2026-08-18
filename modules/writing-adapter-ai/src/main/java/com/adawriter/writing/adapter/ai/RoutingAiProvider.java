package com.adawriter.writing.adapter.ai;

import com.adawriter.writing.domain.AiCompletionCommand;
import com.adawriter.writing.domain.AiCompletionResult;
import com.adawriter.writing.domain.AiProviderException;
import com.adawriter.writing.domain.AiProviderPort;
import com.adawriter.writing.domain.RoutingPreference;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Routes completion requests across ranked providers according to preference, with failover.
 */
public final class RoutingAiProvider implements AiProviderPort {

    private static final Logger log = LoggerFactory.getLogger(RoutingAiProvider.class);

    public record RankedProvider(AiProviderPort provider, int costScore, int latencyScore, int qualityScore) {
        public RankedProvider {
            Objects.requireNonNull(provider, "provider");
        }
    }

    private final List<RankedProvider> providers;
    private final RoutingPreference preference;
    private final AtomicLong routeDecisions = new AtomicLong();
    private final AtomicLong failovers = new AtomicLong();

    public RoutingAiProvider(List<RankedProvider> providers, RoutingPreference preference) {
        Objects.requireNonNull(providers, "providers");
        if (providers.isEmpty()) {
            throw new IllegalArgumentException("providers must not be empty");
        }
        this.providers = List.copyOf(providers);
        this.preference = Objects.requireNonNull(preference, "preference");
    }

    @Override
    public AiCompletionResult complete(AiCompletionCommand command) {
        List<RankedProvider> ordered = orderedProviders();
        AiProviderException last = null;
        boolean first = true;
        for (RankedProvider candidate : ordered) {
            routeDecisions.incrementAndGet();
            if (!first) {
                failovers.incrementAndGet();
            }
            first = false;
            try {
                log.info(
                        "ai_route_selected preference={} provider={}",
                        preference,
                        candidate.provider().providerId());
                return candidate.provider().complete(command);
            } catch (AiProviderException ex) {
                last = ex;
                log.warn(
                        "ai_route_failover from={} reason={}",
                        candidate.provider().providerId(),
                        ex.getMessage());
            }
        }
        throw new AiProviderException("All routed AI providers failed", last);
    }

    @Override
    public String providerId() {
        return "router(" + preference.name().toLowerCase() + ")";
    }

    public long routeDecisions() {
        return routeDecisions.get();
    }

    public long failovers() {
        return failovers.get();
    }

    private List<RankedProvider> orderedProviders() {
        Comparator<RankedProvider> comparator =
                switch (preference) {
                    case COST -> Comparator.comparingInt(RankedProvider::costScore);
                    case LATENCY -> Comparator.comparingInt(RankedProvider::latencyScore);
                    case QUALITY ->
                        Comparator.comparingInt(RankedProvider::qualityScore).reversed();
                };
        return providers.stream().sorted(comparator).toList();
    }
}
