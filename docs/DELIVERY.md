# Phase Execution and Delivery

AdaWriter is delivered **one phase at a time**, each on its own Git branch. A phase is complete only when it is production-ready and has passed all quality gates.

## Mandatory rules

1. Work on **only one phase** at a time.
2. Every phase lives on its own Git branch:
   - `feature/phase-1-foundation`
   - `feature/phase-2-text-detection`
   - `feature/phase-3-ai-engine`
   - `feature/phase-4-desktop-integration`
   - `feature/phase-5-mobile-keyboard`
3. No phase starts until the previous phase has passed all quality gates and is merged (or explicitly accepted).
4. No TODOs, placeholders, unfinished logic, or temporary workarounds.
5. No fake “temporary mocks” in production paths. Test doubles belong only in tests. Supported offline/local providers must be real, intentional adapters.
6. Minimize technical debt; never sacrifice architecture quality for speed.
7. Prioritize correctness over rapid delivery.
8. Follow SOLID, Clean Architecture, DDD, and Secure-by-Design.
9. Every change must be fully tested before merging.

## Product phases

| Phase | Branch | Outcome |
|---|---|---|
| 1 Foundation | `feature/phase-1-foundation` | Build system, hexagonal writing core, localhost agent API, AI provider port, CI/quality gates |
| 2 Text detection | `feature/phase-2-text-detection` | Context/text capture, sensitive-data detection, privacy controls |
| 3 AI engine | `feature/phase-3-ai-engine` | Routing, guardrails depth, prompt lifecycle, cost/latency optimization |
| 4 Desktop integration | `feature/phase-4-desktop-integration` | Native desktop UX/OS integration within performance budgets |
| 5 Mobile keyboard | `feature/phase-5-mobile-keyboard` | Mobile keyboard / companion surfaces |

## Mandatory testing (every phase)

Before merge, the phase must include and pass:

1. **Unit tests** — domain rules, use cases, pure adapters  
2. **Integration tests** — module wiring (e.g. HTTP ↔ use case ↔ AI port)  
3. **Contract tests** — public API shapes stay aligned with OpenAPI / published contracts  
4. **Security checks** — localhost bind, input limits, no secret leakage in logs/responses  
5. **Regression suite** — full `qualityCheck` (format, compile with `-Werror`, tests, coverage reports)  
6. **Manual smoke** — documented curl/script path for the phase’s primary user flow  

Phase-specific additions (performance, a11y, load, device) are required when that phase introduces the relevant surface.

## Merge quality gate

```bash
./gradlew.bat qualityCheck
```

CI on the phase branch must be green. Reviewers verify architecture boundaries and absence of debt markers (`TODO`, `FIXME`, unfinished branches).
