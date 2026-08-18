# ADR 0002: Hexagonal + DDD Module Layout

## Status

Accepted

## Context

We need maintainable boundaries across clients and backends without over-abstracting.

## Decision

Organize each deployable by hexagonal layers and DDD bounded contexts:

```text
…/<context>/
  domain/        # entities, VOs, domain services, ports
  application/   # use cases
  adapters/in/   # REST, gRPC, UI, messaging consumers
  adapters/out/  # DB, AI providers, filesystem, HTTP clients
  bootstrap/     # DI / config composition root
```

Rules:

- Domain has zero framework or vendor SDK dependencies
- Application orchestrates use cases; no UI or persistence details
- Adapters implement ports; swap providers without changing domain
- Public HTTP APIs are specified (OpenAPI) before implementation when external

## Consequences

- Testability via port fakes
- Clear path for local vs cloud AI adapters
- Slightly more ceremony than a single-layer app — justified by multi-client longevity
