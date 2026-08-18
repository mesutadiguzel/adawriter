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
- Aggregate desktop JaCoCo line coverage: **~65.5% → ~83.8% → ~88.3%**.
- Module highlights after slice-2: `writing-domain` 100%, `privacy-application` 100%, `writing-adapter-ai` ~99.5%, `writing-application` ~98.6%, `writing-adapter-rest` ~90.8%, `privacy-domain` ~96.5%.
- Remaining tracked gaps: composition-root `main` methods, AWT gateway wrappers, residual REST stop/interrupt and rare error branches.

## Consequences

- Faster sequential delivery without blocking on routine questions
- Explicit documentation of decisions/risks
- Coverage debt is a tracked finish criterion, not optional
