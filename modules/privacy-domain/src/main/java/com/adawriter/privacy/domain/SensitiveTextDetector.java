package com.adawriter.privacy.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic on-device sensitive text detector.
 *
 * <p>Patterns are intentionally conservative. Findings never expose raw secret values through
 * {@link SensitiveSpan}.
 */
public final class SensitiveTextDetector {

    private static final Map<SensitivityCategory, Pattern> PATTERNS = buildPatterns();

    public DetectionResult detect(String text) {
        if (text == null || text.isEmpty()) {
            return new DetectionResult(text == null ? "" : text, List.of());
        }

        List<SensitiveSpan> spans = new ArrayList<>();
        for (Map.Entry<SensitivityCategory, Pattern> entry : PATTERNS.entrySet()) {
            Matcher matcher = entry.getValue().matcher(text);
            while (matcher.find()) {
                SensitivityCategory category = entry.getKey();
                if (category == SensitivityCategory.CREDIT_CARD
                        && !passesLuhn(matcher.group().replaceAll("\\D", ""))) {
                    continue;
                }
                spans.add(new SensitiveSpan(matcher.start(), matcher.end(), category, "[" + category.name() + "]"));
            }
        }

        spans.sort(
                Comparator.comparingInt(SensitiveSpan::startInclusive).thenComparingInt(SensitiveSpan::endExclusive));
        return new DetectionResult(text, resolveOverlaps(spans));
    }

    public RedactionResult apply(String text, RedactionPolicy policy) {
        DetectionResult detection = detect(text);
        return switch (policy) {
            case REPORT_ONLY -> new RedactionResult(text, detection, policy, false);
            case BLOCK -> {
                if (detection.hasFindings()) {
                    throw new SensitiveContentBlockedException(detection);
                }
                yield new RedactionResult(text, detection, policy, false);
            }
            case REDACT -> new RedactionResult(redact(text, detection.spans()), detection, policy, false);
        };
    }

    private static String redact(String text, List<SensitiveSpan> spans) {
        if (spans.isEmpty()) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length());
        int cursor = 0;
        for (SensitiveSpan span : spans) {
            out.append(text, cursor, span.startInclusive());
            out.append(span.redactionToken());
            cursor = span.endExclusive();
        }
        out.append(text, cursor, text.length());
        return out.toString();
    }

    private static List<SensitiveSpan> resolveOverlaps(List<SensitiveSpan> sorted) {
        if (sorted.isEmpty()) {
            return List.of();
        }
        List<SensitiveSpan> resolved = new ArrayList<>();
        SensitiveSpan current = sorted.getFirst();
        for (int i = 1; i < sorted.size(); i++) {
            SensitiveSpan next = sorted.get(i);
            if (next.startInclusive() < current.endExclusive()) {
                if (next.length() > current.length()) {
                    current = next;
                }
            } else {
                resolved.add(current);
                current = next;
            }
        }
        resolved.add(current);
        return List.copyOf(resolved);
    }

    private static boolean passesLuhn(String digits) {
        if (digits.length() < 13 || digits.length() > 19) {
            return false;
        }
        int sum = 0;
        boolean alternate = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n -= 9;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    private static Map<SensitivityCategory, Pattern> buildPatterns() {
        Map<SensitivityCategory, Pattern> map = new EnumMap<>(SensitivityCategory.class);
        map.put(
                SensitivityCategory.EMAIL,
                Pattern.compile("\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b", Pattern.CASE_INSENSITIVE));
        map.put(
                SensitivityCategory.PHONE,
                Pattern.compile("(?<!\\w)(?:\\+?1[-.\\s]?)?(?:\\(?\\d{3}\\)?[-.\\s]?)\\d{3}[-.\\s]?\\d{4}(?!\\w)"));
        map.put(SensitivityCategory.CREDIT_CARD, Pattern.compile("(?<!\\d)(?:\\d[ -]*?){13,19}(?!\\d)"));
        map.put(SensitivityCategory.US_SSN, Pattern.compile("(?<!\\d)\\d{3}-\\d{2}-\\d{4}(?!\\d)"));
        map.put(
                SensitivityCategory.API_KEY,
                Pattern.compile(
                        "(?i)\\b(?:sk|rk|pk|api)[_-][A-Za-z0-9_]{16,}\\b|\\b(?:ghp|gho|ghu|ghs|ghr)_[A-Za-z0-9]{20,}\\b"));
        map.put(
                SensitivityCategory.PRIVATE_KEY_BLOCK,
                Pattern.compile(
                        "-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----[\\s\\S]+?-----END (?:RSA |EC |OPENSSH )?PRIVATE KEY-----"));
        map.put(
                SensitivityCategory.IPV4,
                Pattern.compile(
                        "\\b(?:(?:25[0-5]|2[0-4]\\d|[01]?\\d?\\d)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d?\\d)\\b"));
        return Map.copyOf(map);
    }
}
