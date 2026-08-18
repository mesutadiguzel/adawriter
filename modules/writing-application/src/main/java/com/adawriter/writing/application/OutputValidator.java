package com.adawriter.writing.application;

import com.adawriter.writing.domain.ValidationException;
import com.adawriter.writing.domain.WritingConstraints;

/**
 * Basic output validation / guardrails for model responses.
 */
public final class OutputValidator {

    private OutputValidator() {}

    public static String validate(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ValidationException("AI provider returned empty output");
        }
        String trimmed = stripCodeFences(raw.trim());
        if (trimmed.isBlank()) {
            throw new ValidationException("AI provider returned blank output after normalization");
        }
        if (trimmed.length() > WritingConstraints.MAX_OUTPUT_CHARS) {
            throw new ValidationException("AI provider output exceeds size limit");
        }
        return trimmed;
    }

    private static String stripCodeFences(String text) {
        if (text.startsWith("```") && text.endsWith("```")) {
            int firstNewline = text.indexOf('\n');
            if (firstNewline > 0) {
                return text.substring(firstNewline + 1, text.length() - 3).trim();
            }
        }
        return text;
    }
}
