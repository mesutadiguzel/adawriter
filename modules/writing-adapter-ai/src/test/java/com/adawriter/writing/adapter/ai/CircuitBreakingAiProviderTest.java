package com.adawriter.writing.adapter.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.adawriter.writing.domain.AiCompletionCommand;
import com.adawriter.writing.domain.AiCompletionResult;
import com.adawriter.writing.domain.AiProviderException;
import com.adawriter.writing.domain.AiProviderPort;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class CircuitBreakingAiProviderTest {

    @Test
    void opensAfterThresholdFailures() {
        AtomicInteger calls = new AtomicInteger();
        AiProviderPort failing = new AiProviderPort() {
            @Override
            public AiCompletionResult complete(AiCompletionCommand command) {
                calls.incrementAndGet();
                throw new AiProviderException("down");
            }

            @Override
            public String providerId() {
                return "failing";
            }
        };

        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        CircuitBreakingAiProvider breaker = new CircuitBreakingAiProvider(failing, 2, Duration.ofSeconds(30), clock);

        assertThatThrownBy(() -> breaker.complete(cmd())).isInstanceOf(AiProviderException.class);
        assertThatThrownBy(() -> breaker.complete(cmd())).isInstanceOf(AiProviderException.class);
        assertThat(breaker.openEvents()).isEqualTo(1);

        assertThatThrownBy(() -> breaker.complete(cmd()))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("Circuit open");
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void negative_rejectsInvalidThreshold() {
        assertThatThrownBy(() -> new CircuitBreakingAiProvider(providerOk(), 0, Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("failureThreshold");
    }

    @Test
    void positive_successResetsFailureCount() {
        AtomicInteger calls = new AtomicInteger();
        AiProviderPort flaky = new AiProviderPort() {
            @Override
            public AiCompletionResult complete(AiCompletionCommand command) {
                int n = calls.incrementAndGet();
                if (n == 1) {
                    throw new AiProviderException("transient");
                }
                return new AiCompletionResult("ok", "m", 1L);
            }

            @Override
            public String providerId() {
                return "flaky";
            }
        };
        CircuitBreakingAiProvider breaker = new CircuitBreakingAiProvider(flaky, 3, Duration.ofSeconds(30));
        assertThatThrownBy(() -> breaker.complete(cmd())).isInstanceOf(AiProviderException.class);
        assertThat(breaker.complete(cmd()).text()).isEqualTo("ok");
        assertThat(breaker.openEvents()).isZero();
    }

    private static AiProviderPort providerOk() {
        return new AiProviderPort() {
            @Override
            public AiCompletionResult complete(AiCompletionCommand command) {
                return new AiCompletionResult("ok", "m", 1L);
            }

            @Override
            public String providerId() {
                return "ok";
            }
        };
    }

    private static AiCompletionCommand cmd() {
        return new AiCompletionCommand("sys", "user", 64);
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
