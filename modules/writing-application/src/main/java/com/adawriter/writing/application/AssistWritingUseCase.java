package com.adawriter.writing.application;

import com.adawriter.privacy.application.PrivacyGuard;
import com.adawriter.privacy.domain.RedactionResult;
import com.adawriter.privacy.domain.SensitiveContentBlockedException;
import com.adawriter.writing.domain.AiCompletionCommand;
import com.adawriter.writing.domain.AiCompletionResult;
import com.adawriter.writing.domain.AiProviderException;
import com.adawriter.writing.domain.AiProviderPort;
import com.adawriter.writing.domain.UnexpectedWritingException;
import com.adawriter.writing.domain.ValidationException;
import com.adawriter.writing.domain.WritingConstraints;
import com.adawriter.writing.domain.WritingRequest;
import com.adawriter.writing.domain.WritingResult;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application service: assist writing via the AI provider port.
 */
public final class AssistWritingUseCase {

    private static final Logger log = LoggerFactory.getLogger(AssistWritingUseCase.class);

    private final AiProviderPort aiProvider;
    private final WritingMetrics metrics;
    private final PrivacyGuard privacyGuard;

    public AssistWritingUseCase(AiProviderPort aiProvider, WritingMetrics metrics, PrivacyGuard privacyGuard) {
        this.aiProvider = Objects.requireNonNull(aiProvider, "aiProvider");
        this.metrics = Objects.requireNonNull(metrics, "metrics");
        this.privacyGuard = Objects.requireNonNull(privacyGuard, "privacyGuard");
    }

    public WritingResult execute(WritingRequest request) {
        Objects.requireNonNull(request, "request");
        try {
            RedactionResult protectedText = privacyGuard.protectForAssist(request.text());
            WritingRequest safeRequest = request.withText(protectedText.text());

            AiCompletionCommand command = new AiCompletionCommand(
                    PromptCatalog.systemPrompt(),
                    PromptCatalog.userPrompt(safeRequest),
                    WritingConstraints.DEFAULT_MAX_TOKENS);
            AiCompletionResult completion = aiProvider.complete(command);
            String validated = OutputValidator.validate(completion.text());
            WritingResult result = new WritingResult(
                    validated,
                    aiProvider.providerId(),
                    completion.modelId(),
                    PromptCatalog.VERSION,
                    completion.latencyMs());
            metrics.recordSuccess(result.latencyMs());
            log.info(
                    "assist_success provider={} model={} action={} latencyMs={} promptVersion={} privacyFindings={}",
                    result.providerId(),
                    result.modelId(),
                    request.action(),
                    result.latencyMs(),
                    result.promptVersion(),
                    protectedText.detection().findingCount());
            return result;
        } catch (SensitiveContentBlockedException | ValidationException | AiProviderException ex) {
            metrics.recordFailure();
            log.warn(
                    "assist_failure provider={} action={} type={} reason={}",
                    aiProvider.providerId(),
                    request.action(),
                    ex.getClass().getSimpleName(),
                    ex.getMessage());
            throw ex;
        } catch (RuntimeException ex) {
            metrics.recordFailure();
            log.warn(
                    "assist_failure provider={} action={} type={} reason={}",
                    aiProvider.providerId(),
                    request.action(),
                    ex.getClass().getSimpleName(),
                    ex.getMessage());
            throw new UnexpectedWritingException("Unexpected writing assistance failure", ex);
        }
    }
}
