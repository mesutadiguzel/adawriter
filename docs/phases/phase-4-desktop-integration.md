# Phase 4 — Desktop Integration

Branch: `feature/phase-4-desktop-integration`

## Scope

- In-process desktop shell that hosts the local agent
- System tray actions for clipboard assist (rewrite/shorten/expand/fix grammar)
- Direct use-case path (no extra network hop) with privacy + AI engine intact
- Headless-safe behavior when tray is unavailable (agent API still runs)

## Exit criteria

- [x] Desktop shell module wired to existing hexagonal core
- [x] Clipboard assist service fully tested without GUI
- [ ] `./gradlew qualityCheck` passes
- [ ] PR opened
