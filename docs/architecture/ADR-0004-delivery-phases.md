# ADR 0004: Delivery Phases

## Status

Accepted (supersedes earlier Phase 0–4 cloud-scale roadmap for near-term delivery)

## Context

AdaWriter needs a strict phase-by-phase delivery model with one active branch per phase and production-ready exits.

## Decision

Deliver in five product phases:

1. **Foundation** (`feature/phase-1-foundation`) — hexagonal writing core, desktop agent localhost API, AI provider abstraction, CI/quality gates  
2. **Text detection** (`feature/phase-2-text-detection`) — capture/detection, sensitive content protection  
3. **AI engine** (`feature/phase-3-ai-engine`) — advanced routing, guardrails, prompt lifecycle  
4. **Desktop integration** (`feature/phase-4-desktop-integration`) — native desktop experience under CPU/memory budgets  
5. **Mobile keyboard** (`feature/phase-5-mobile-keyboard`) — mobile input surface  

Enterprise multi-region scale remains a later program track after Phase 5, not a blocker for these phases.

## Consequences

- Clear branch ownership and exit criteria per phase  
- Earlier phases stay lean; later phases extend ports without rewriting domain  
- See `docs/DELIVERY.md` for gates and testing mandates  
