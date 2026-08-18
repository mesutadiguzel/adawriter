# Phase 5 — Mobile Keyboard

Branch: `feature/phase-5-mobile-keyboard`

## Scope

- Android IME (`mobile/keyboard`) using shared hexagonal writing + privacy core
- On-device offline AI provider by default (local-first)
- Assist actions: rewrite, shorten, expand, fix grammar
- Unit tests for keyboard assist facade

## Exit criteria

- [x] IME service + input view + method XML
- [x] Shared core reused (no duplicated domain logic)
- [ ] `./gradlew qualityCheck` passes
- [ ] PR opened
