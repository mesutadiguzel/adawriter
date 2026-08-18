# Autonomous Delivery Mode

AdaWriter is executed under autonomous ownership by the engineering agent acting as Principal Engineer, Chief Architect, QA Director, Security Architect, DevOps Lead, Product Owner, and Technical Program Manager.

Routine implementation details do not require confirmation. Decisions, risks, and rationale must be documented.

## Execution rules

- Work phases sequentially; finish the current focus before switching
- Make reasonable engineering decisions from best practices
- Identify and resolve risks/architecture issues proactively
- Continuously optimize architecture, performance, security, maintainability, scalability
- Never knowingly leave design flaws unresolved

## Quality enforcement

Never assume code is correct. Verify with evidence.

For every feature: validate requirements, run tests, verify behavior, regression, security, and performance as applicable to the surface.

**Coverage rule:** aim for **100% JaCoCo line coverage** when the application is finished. Interim gaps must be tracked and closed.

## Testing strategy

Assume every change can introduce defects. Prefer automated unit, integration, API, regression, and security-oriented checks on every change. Expand to e2e/load/stress/perf/memory/CPU/cross-platform as those surfaces exist.

Every defect: document → fix → retest → regression.

## Git workflow

For each unit of work: feature branch → implement → tests → validation → PR → merge after gates.

```bash
./gradlew.bat desktopQualityCheck
```

## Performance / security / refactoring

Desktop targets: startup &lt; 3s, idle CPU &lt; 1%, active CPU &lt; 5%, memory &lt; 250 MB.

Security by default: no secrets in repo, localhost bind, data minimization, prompt-injection defenses, dependency hygiene.

Refactor when complexity, duplication, or maintainability degrades. Follow SOLID, DRY, KISS, YAGNI, Clean Architecture, DDD, Secure-by-Design.

## Definition of done

Requirements, tests passing, security/performance considerations addressed for the surface, docs updated, no critical defects, production-ready for that slice.

## Current focus (active)

**Desktop product quality** (mobile deferred): raise coverage toward 100%, harden desktop-shell/agent, keep architecture clean. Latest measured aggregate after coverage-hardening slice: **~83.8%** line coverage.
