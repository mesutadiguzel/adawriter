# ADR 0006: Autonomous delivery + desktop coverage push

## Status

Accepted

## Context

AdaWriter now has Phases 1–5 merged. Desktop is the active product focus. An autonomous delivery operating model was adopted. Finished-product coverage goal remains **100% JaCoCo line coverage**.

## Decision

1. Operate under `docs/AUTONOMOUS_DELIVERY.md` for execution, quality, git, security, and DoD.
2. Implementation slice: **desktop coverage & quality** on `feature/desktop-coverage-hardening`.
3. Share `DesktopRuntimeConfig` in `writing-application` for agent + shell parity.
4. Make AI factory / OpenAI HTTP adapter testable via env functions and injectable `HttpClient`.
5. Extract `SystemTrayGateway` / `ClipboardGateway` so tray logic is unit-tested without real AWT tray.
6. Mobile remains deferred until desktop meets stronger coverage/quality evidence.

## Evidence (this slice)

- Gate: `./gradlew.bat desktopQualityCheck` passes.
- Aggregate desktop JaCoCo line coverage: **~65.5% → ~83.8%** (698/833 lines).
- Module highlights: `writing-domain` 100%, `privacy-application` 100%, `writing-adapter-ai` ~91%, `desktop-shell` ~55% (tray controller ~96%; AWT gateways + `main` remain).
- Remaining tracked gaps: `DesktopShellMain` / agent `main`, AWT gateway wrappers, residual REST/AI/application branches.

## Consequences

- Faster sequential delivery without blocking on routine questions
- Explicit documentation of decisions/risks
- Coverage debt is a tracked finish criterion, not optional
