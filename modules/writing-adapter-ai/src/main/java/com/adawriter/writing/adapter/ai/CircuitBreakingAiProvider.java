package com.adawriter.writing.adapter.ai;

import com.adawriter.writing.domain.AiCompletionCommand;
import com.adawriter.writing.domain.AiCompletionResult;
import com.adawriter.writing.domain.AiProviderException;
import com.adawriter.writing.domain.AiProviderPort;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Circuit breaker around an {@link AiProviderPort}.
 */
public final class CircuitBreakingAiProvider implements AiProviderPort {

    private final AiProviderPort delegate;
    private final int failureThreshold;
    private final Duration openDuration;
    private final Clock clock;
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private final AtomicReference<Instant> openUntil = new AtomicReference<>(Instant.EPOCH);
    private final AtomicInteger openEvents = new AtomicInteger();

    public CircuitBreakingAiProvider(AiProviderPort delegate, int failureThreshold, Duration openDuration) {
        this(delegate, failureThreshold, openDuration, Clock.systemUTC());
    }

    CircuitBreakingAiProvider(AiProviderPort delegate, int failureThreshold, Duration openDuration, Clock clock) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        if (failureThreshold < 1) {
            throw new IllegalArgumentException("failureThreshold must be >= 1");
        }
        this.failureThreshold = failureThreshold;
        this.openDuration = Objects.requireNonNull(openDuration, "openDuration");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public AiCompletionResult complete(AiCompletionCommand command) {
        Instant now = clock.instant();
        Instant blockedUntil = openUntil.get();
        if (now.isBefore(blockedUntil)) {
            throw new AiProviderException("Circuit open for provider " + providerId() + " until " + blockedUntil);
        }
        try {
            AiCompletionResult result = delegate.complete(command);
            consecutiveFailures.set(0);
            return result;
        } catch (AiProviderException ex) {
            int failures = consecutiveFailures.incrementAndGet();
            if (failures >= failureThreshold) {
                openUntil.set(now.plus(openDuration));
                openEvents.incrementAndGet();
                consecutiveFailures.set(0);
            }
            throw ex;
        }
    }

    @Override
    public String providerId() {
        return delegate.providerId();
    }

    public int openEvents() {
        return openEvents.get();
    }
}
