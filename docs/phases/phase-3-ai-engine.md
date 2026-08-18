# Phase 3 — AI Engine

Branch: `feature/phase-3-ai-engine`

## Scope

- Cost/latency/quality-aware model routing (`ADAWRITER_AI_PROVIDER=routed`)
- Provider circuit breaker + failover orchestration
- Prompt registry (`writing-assist-v2`) with retained v1
- Output guardrails (injection echo + repetition)
- AI observability (route logs, estimated tokens)

## Exit criteria

- [x] Router selects providers deterministically from policy
- [x] Circuit breaker opens after consecutive failures
- [x] Prompt versions explicit and test-covered
- [x] `./gradlew qualityCheck` passes
- [ ] PR opened
