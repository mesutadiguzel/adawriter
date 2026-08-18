package com.adawriter.writing.adapter.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.adawriter.writing.domain.AiCompletionCommand;
import com.adawriter.writing.domain.AiCompletionResult;
import com.adawriter.writing.domain.AiProviderException;
import com.adawriter.writing.domain.AiProviderPort;
import com.adawriter.writing.domain.RoutingPreference;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RoutingAiProviderTest {

    @Test
    void prefersLowerCostProvider() {
        AtomicInteger cheapCalls = new AtomicInteger();
        AiProviderPort cheap = provider("cheap", cheapCalls, "cheap-out");
        AiProviderPort expensive = provider("expensive", new AtomicInteger(), "expensive-out");

        RoutingAiProvider router = new RoutingAiProvider(
                List.of(
                        new RoutingAiProvider.RankedProvider(expensive, 90, 10, 90),
                        new RoutingAiProvider.RankedProvider(cheap, 10, 50, 40)),
                RoutingPreference.COST);

        AiCompletionResult result = router.complete(new AiCompletionCommand("sys", "user", 64));
        assertThat(result.text()).isEqualTo("cheap-out");
        assertThat(cheapCalls.get()).isEqualTo(1);
    }

    @Test
    void failsOverWhenPreferredProviderFails() {
        AiProviderPort primary = new AiProviderPort() {
            @Override
            public AiCompletionResult complete(AiCompletionCommand command) {
                throw new AiProviderException("primary down");
            }

            @Override
            public String providerId() {
                return "primary";
            }
        };
        AiProviderPort secondary = provider("secondary", new AtomicInteger(), "ok");

        RoutingAiProvider router = new RoutingAiProvider(
                List.of(
                        new RoutingAiProvider.RankedProvider(primary, 1, 1, 100),
                        new RoutingAiProvider.RankedProvider(secondary, 50, 50, 50)),
                RoutingPreference.COST);

        assertThat(router.complete(new AiCompletionCommand("sys", "user", 64)).text())
                .isEqualTo("ok");
        assertThat(router.failovers()).isEqualTo(1);
    }

    private static AiProviderPort provider(String id, AtomicInteger calls, String output) {
        return new AiProviderPort() {
            @Override
            public AiCompletionResult complete(AiCompletionCommand command) {
                calls.incrementAndGet();
                return new AiCompletionResult(output, id + "-model", 1L);
            }

            @Override
            public String providerId() {
                return id;
            }
        };
    }
}
