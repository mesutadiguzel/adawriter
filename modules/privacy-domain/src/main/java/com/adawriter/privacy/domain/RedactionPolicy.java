package com.adawriter.privacy.domain;

/**
 * How writing assistance should treat detected sensitive content before AI egress.
 */
public enum RedactionPolicy {
    /** Replace sensitive spans with category tokens and continue. */
    REDACT,
    /** Refuse the operation when any sensitive span is found. */
    BLOCK,
    /** Detect only; do not alter text (for inspect endpoints). */
    REPORT_ONLY
}
