package com.adawriter.writing.application;

import com.adawriter.writing.domain.ValidationException;
import com.adawriter.writing.domain.WritingConstraints;
import java.util.Locale;

/**
 * Output guardrails for model responses.
 */
public final class OutputGuardrails {

    private OutputGuardrails() {}

    public static String enforce(String raw) {
        String validated = OutputValidator.validate(raw);
        String normalized = validated.trim();
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.contains("ignore previous instructions")
                || lower.contains("system prompt")
                || lower.contains("developer message")) {
            throw new ValidationException("AI output failed prompt-injection guardrail");
        }
        if (hasExcessiveRepetition(normalized)) {
            throw new ValidationException("AI output failed repetition guardrail");
        }
        if (normalized.length() > WritingConstraints.MAX_OUTPUT_CHARS) {
            throw new ValidationException("AI provider output exceeds size limit");
        }
        return normalized;
    }

    private static boolean hasExcessiveRepetition(String text) {
        if (text.length() < 40) {
            return false;
        }
        String sample = text.substring(0, Math.min(20, text.length()));
        int occurrences = 0;
        int idx = 0;
        while ((idx = text.indexOf(sample, idx)) >= 0) {
            occurrences++;
            idx += sample.length();
            if (occurrences >= 8) {
                return true;
            }
        }
        return false;
    }
}
