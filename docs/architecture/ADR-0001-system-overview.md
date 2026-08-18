# ADR 0001: System Overview

## Status

Accepted

## Context

AdaWriter must be a privacy-first AI writing platform that runs continuously on desktop, browser, mobile, and enterprise environments with negligible device impact.

## Decision

Adopt a **local-first core + optional cloud control plane** architecture:

| Component | Responsibility |
|---|---|
| Desktop Agent | Local capture/assist surface; primary runtime; stays within CPU/memory budgets |
| Browser Extension | Thin client talking to agent or secure API; minimal page impact |
| Mobile App | Lightweight companion with same domain contracts |
| Platform API | Auth, sync, billing, team/enterprise policies (optional when online) |
| AI Gateway | Provider abstraction, routing, guardrails, observability |

Bounded contexts (initial):

1. **Writing Assistance** — drafting, rewrite, tone, suggestions
2. **Identity & Access** — users, orgs, RBAC, MFA
3. **Privacy & Consent** — retention, deletion, sensitive-data handling
4. **AI Orchestration** — prompts, models, routing, validation
5. **Sync & Tenancy** — optional multi-device / multi-tenant sync

## Consequences

- Domain logic lives once and is shared via clear ports/contracts
- Offline-capable UX without blocking on cloud availability
- Cloud features can scale horizontally without forcing all processing off-device
- Must invest early in AI provider ports and privacy controls

## Non-goals (YAGNI for v1)

Global multi-region active-active, full marketplace plugins, and every enterprise connector — designed for later, not built until needed.
