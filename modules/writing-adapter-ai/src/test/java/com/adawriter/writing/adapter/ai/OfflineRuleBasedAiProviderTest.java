package com.adawriter.writing.adapter.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.adawriter.writing.domain.AiCompletionCommand;
import com.adawriter.writing.domain.AiCompletionResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class OfflineRuleBasedAiProviderTest {

    private final OfflineRuleBasedAiProvider provider = new OfflineRuleBasedAiProvider();

    @ParameterizedTest
    @CsvSource({
        "REWRITE, hello world, Hello world",
        "SHORTEN, One. Two., One.",
        "EXPAND, Idea., 'Idea. Additionally, this draft expands the idea with clearer supporting detail.'",
        "FIX_GRAMMAR, 'yes i can', 'yes I can'"
    })
    void positive_transformsByAction(String action, String document, String expected) {
        AiCompletionResult result = provider.complete(command(action, document));
        assertThat(result.text()).isEqualTo(expected);
        assertThat(result.modelId()).isEqualTo(OfflineRuleBasedAiProvider.MODEL_ID);
        assertThat(provider.providerId()).isEqualTo(OfflineRuleBasedAiProvider.PROVIDER_ID);
    }

    @Test
    void positive_changeToneReturnsTrimmedDocument() {
        AiCompletionResult result = provider.complete(command("CHANGE_TONE", "  Keep calm  "));
        assertThat(result.text()).isEqualTo("Keep calm");
    }

    @Test
    void positive_changeToneProfessionalAndCasual() {
        assertThat(provider.complete(commandWithTone("CHANGE_TONE", "Doc", "professional"))
                        .text())
                .isEqualTo("Doc");
        assertThat(provider.complete(commandWithTone("CHANGE_TONE", "Doc", "casual"))
                        .text())
                .isEqualTo("Doc");
    }

    @Test
    void positive_rewriteEmptyDocument() {
        assertThat(provider.complete(command("REWRITE", "")).text()).isEmpty();
    }

    @Test
    void positive_shortenSingleSentenceUnchanged() {
        assertThat(provider.complete(command("SHORTEN", "Only one.")).text()).isEqualTo("Only one.");
    }

    @Test
    void positive_extractsWithoutDelimiters() {
        AiCompletionResult result =
                provider.complete(new AiCompletionCommand("sys", "Action: REWRITE\nhello world", 64));
        assertThat(result.text()).isEqualTo("Action: REWRITE\nhello world");
    }

    private static AiCompletionCommand command(String action, String document) {
        String userPrompt =
                """
                Action: %s
                Locale: en
                Document:
                ---
                %s
                ---
                """
                        .formatted(action, document);
        return new AiCompletionCommand("system", userPrompt, 128);
    }

    private static AiCompletionCommand commandWithTone(String action, String document, String tone) {
        String userPrompt =
                """
                Action: %s
                Tone: %s
                Locale: en
                Document:
                ---
                %s
                ---
                """
                        .formatted(action, tone, document);
        return new AiCompletionCommand("system", userPrompt, 128);
    }
}
