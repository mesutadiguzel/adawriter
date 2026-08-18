package com.adawriter.writing.application;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Minimal in-process metrics for Phase 1 observability.
 */
public final class WritingMetrics {

    private final AtomicLong assistRequests = new AtomicLong();
    private final AtomicLong assistFailures = new AtomicLong();
    private final AtomicLong totalLatencyMs = new AtomicLong();

    public void recordSuccess(long latencyMs) {
        assistRequests.incrementAndGet();
        totalLatencyMs.addAndGet(latencyMs);
    }

    public void recordFailure() {
        assistRequests.incrementAndGet();
        assistFailures.incrementAndGet();
    }

    public long assistRequests() {
        return assistRequests.get();
    }

    public long assistFailures() {
        return assistFailures.get();
    }

    public long totalLatencyMs() {
        return totalLatencyMs.get();
    }
}
