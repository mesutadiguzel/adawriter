# ADR 0005: Phase 1 Technology Stack

## Status

Accepted

## Context

Phase 1 needs a local writing core with hexagonal boundaries, an AI provider port, and a desktop agent that stays within performance budgets.

## Decision

| Layer | Choice | Why |
|---|---|---|
| Language | Java 21 (LTS toolchain) | Stable LTS; available in this workspace |
| Build | Gradle multi-module | Fast incremental builds; clear module boundaries |
| Architecture | Hexagonal modules under `modules/` + `apps/desktop-agent` | Matches ADR-0002 |
| HTTP (local agent) | JDK `HttpServer` | Zero extra server framework weight |
| JSON | Jackson | Practical for REST without overbuilding |
| HTTP client (AI) | `java.net.http.HttpClient` | Built-in; enough for OpenAI-compatible APIs |
| Logging | SLF4J + Logback JSON-friendly pattern | Structured logs without a heavy stack |
| Metrics | Lightweight in-process counters | Enough for Phase 1; Micrometer later if needed |
| Tests | JUnit 5 + AssertJ | Unit + integration |
| Quality | Spotless, JaCoCo, `-Werror`, GitHub Actions CI | Merge gates |

Module map:

```text
modules/writing-domain
modules/writing-application
modules/writing-adapter-ai
modules/writing-adapter-rest
apps/desktop-agent
```

## Consequences

- Desktop UI shell (Tauri/JavaFX) deferred; agent exposes localhost API first
- Cloud AI is optional via env config; offline rule-based provider works without network by default
- Can swap Quarkus/native or a richer agent UI later without rewriting domain
