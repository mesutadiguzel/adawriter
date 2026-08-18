# Phase 2 — Text Detection

Branch: `feature/phase-2-text-detection`

## Scope

- Sensitive text detection (email, phone, card+Luhn, SSN, API keys, private keys, IPv4)
- Redaction policies (`REDACT`, `BLOCK`, `REPORT_ONLY`)
- Privacy guard integrated into assist pipeline before AI egress
- REST endpoints: `POST /v1/privacy/detect`, `POST /v1/privacy/redact`
- OpenAPI + tests

## Exit criteria

- [x] Deterministic on-device detector with unit tests
- [x] Assist path redacts sensitive content before provider calls
- [x] Findings never return raw secret values
- [x] No TODOs / placeholders
- [x] `./gradlew qualityCheck` passes
- [ ] Branch pushed / PR opened
