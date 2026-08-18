package com.adawriter.privacy.domain;

/**
 * Categories of sensitive content detected in user text.
 */
public enum SensitivityCategory {
    EMAIL,
    PHONE,
    CREDIT_CARD,
    US_SSN,
    API_KEY,
    PRIVATE_KEY_BLOCK,
    IPV4
}
