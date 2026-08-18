package com.adawriter.writing.application;

import com.adawriter.privacy.domain.RedactionPolicy;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

/**
 * Shared runtime configuration parsing for desktop agent/shell.
 */
public final class DesktopRuntimeConfig {

    private final int port;
    private final RedactionPolicy assistPrivacyPolicy;

    public DesktopRuntimeConfig(int port, RedactionPolicy assistPrivacyPolicy) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port out of range: " + port);
        }
        this.port = port;
        this.assistPrivacyPolicy = Objects.requireNonNull(assistPrivacyPolicy, "assistPrivacyPolicy");
        if (assistPrivacyPolicy == RedactionPolicy.REPORT_ONLY) {
            throw new IllegalArgumentException("assist privacy policy must be REDACT or BLOCK");
        }
    }

    public static DesktopRuntimeConfig fromEnvironment() {
        return from(System::getenv);
    }

    public static DesktopRuntimeConfig from(Function<String, String> env) {
        Objects.requireNonNull(env, "env");
        int port = parsePort(value(env, "ADAWRITER_PORT", "8787"));
        RedactionPolicy policy = parseAssistPolicy(value(env, "ADAWRITER_PRIVACY_POLICY", "REDACT"));
        return new DesktopRuntimeConfig(port, policy);
    }

    public int port() {
        return port;
    }

    public RedactionPolicy assistPrivacyPolicy() {
        return assistPrivacyPolicy;
    }

    public static int parsePort(String raw) {
        try {
            int port = Integer.parseInt(raw.trim());
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("ADAWRITER_PORT out of range: " + raw);
            }
            return port;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid ADAWRITER_PORT: " + raw, ex);
        }
    }

    public static RedactionPolicy parseAssistPolicy(String raw) {
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        final RedactionPolicy policy;
        try {
            policy = RedactionPolicy.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid ADAWRITER_PRIVACY_POLICY: " + raw, ex);
        }
        if (policy == RedactionPolicy.REPORT_ONLY) {
            throw new IllegalArgumentException("ADAWRITER_PRIVACY_POLICY for assist must be REDACT or BLOCK");
        }
        return policy;
    }

    private static String value(Function<String, String> env, String key, String defaultValue) {
        String value = env.apply(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
