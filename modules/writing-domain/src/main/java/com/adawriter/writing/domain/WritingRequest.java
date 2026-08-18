package com.adawriter.writing.domain;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable request to assist with a piece of text.
 */
public final class WritingRequest {

    private final String text;
    private final WritingAction action;
    private final WritingTone tone;
    private final String locale;

    private WritingRequest(String text, WritingAction action, WritingTone tone, String locale) {
        this.text = text;
        this.action = action;
        this.tone = tone;
        this.locale = locale;
    }

    public static WritingRequest of(String text, WritingAction action) {
        return builder(text, action).build();
    }

    public static Builder builder(String text, WritingAction action) {
        return new Builder(text, action);
    }

    public String text() {
        return text;
    }

    public WritingAction action() {
        return action;
    }

    public Optional<WritingTone> tone() {
        return Optional.ofNullable(tone);
    }

    public String locale() {
        return locale;
    }

    public static final class Builder {
        private final String text;
        private final WritingAction action;
        private WritingTone tone;
        private String locale = "en";

        private Builder(String text, WritingAction action) {
            this.text = requireText(text);
            this.action = Objects.requireNonNull(action, "action");
        }

        public Builder tone(WritingTone tone) {
            this.tone = tone;
            return this;
        }

        public Builder locale(String locale) {
            this.locale = requireNonBlank(locale, "locale");
            return this;
        }

        public WritingRequest build() {
            if (action == WritingAction.CHANGE_TONE && tone == null) {
                throw new ValidationException("tone is required for CHANGE_TONE");
            }
            return new WritingRequest(text, action, tone, locale);
        }

        private static String requireText(String value) {
            String normalized = requireNonBlank(value, "text");
            if (normalized.length() > WritingConstraints.MAX_TEXT_CHARS) {
                throw new ValidationException("text exceeds max length of " + WritingConstraints.MAX_TEXT_CHARS);
            }
            return normalized;
        }

        private static String requireNonBlank(String value, String name) {
            Objects.requireNonNull(value, name);
            if (value.isBlank()) {
                throw new ValidationException(name + " must not be blank");
            }
            return value;
        }
    }
}
