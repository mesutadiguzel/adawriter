# Phase 1 — Foundation

Branch: `feature/phase-1-foundation`

## Scope

- Gradle multi-module hexagonal layout
- Writing domain + application use case
- AI provider port with offline + OpenAI-compatible + failover adapters
- Localhost desktop agent API (`127.0.0.1`)
- OpenAPI contract, CI, Spotless, JaCoCo, security notes
- Delivery process documentation

## Exit criteria

- [x] Architecture boundaries respected (domain has no framework/vendor deps)
- [x] No TODO/FIXME/placeholder debt in production paths
- [x] Offline provider is a real local-first adapter (not a test mock)
- [x] `./gradlew qualityCheck` passes
- [x] Smoke path documented in README (`POST /v1/assist`)
- [x] Security baseline documented (`docs/SECURITY.md`)

## Out of scope (later phases)

- OS text capture / detection (Phase 2)
- Advanced AI routing & prompt lifecycle (Phase 3)
- Native desktop UI shell (Phase 4)
- Mobile keyboard (Phase 5)
