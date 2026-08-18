package com.adawriter.writing.adapter.ai;

import com.adawriter.writing.domain.AiCompletionCommand;
import com.adawriter.writing.domain.AiCompletionResult;
import com.adawriter.writing.domain.AiProviderException;
import com.adawriter.writing.domain.AiProviderPort;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tries primary providers in order and fails over on {@link AiProviderException}.
 */
public final class FallbackAiProvider implements AiProviderPort {

    private static final Logger log = LoggerFactory.getLogger(FallbackAiProvider.class);

    private final List<AiProviderPort> providers;

    public FallbackAiProvider(List<AiProviderPort> providers) {
        Objects.requireNonNull(providers, "providers");
        if (providers.isEmpty()) {
            throw new IllegalArgumentException("providers must not be empty");
        }
        this.providers = List.copyOf(providers);
    }

    @Override
    public AiCompletionResult complete(AiCompletionCommand command) {
        AiProviderException lastFailure = null;
        for (AiProviderPort provider : providers) {
            try {
                return provider.complete(command);
            } catch (AiProviderException ex) {
                lastFailure = ex;
                log.warn("ai_provider_failover from={} reason={}", provider.providerId(), ex.getMessage());
            }
        }
        throw new AiProviderException("All AI providers failed", lastFailure == null ? null : lastFailure);
    }

    @Override
    public String providerId() {
        return "fallback("
                + String.join(
                        ",", providers.stream().map(AiProviderPort::providerId).toList())
                + ")";
    }
}
