# AdaWriter

Privacy-first AI writing platform designed to feel lightweight, secure, and fast on desktop, mobile, browser, and enterprise environments.

## Engineering standards

Every decision ranks: **UX → Performance → Security → Simplicity → Reliability → Maintainability → Scalability → Cost**.

- Requirements: [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md)
- Delivery process: [docs/DELIVERY.md](docs/DELIVERY.md)
- Architecture ADRs: [docs/architecture/](docs/architecture/)
- Phase 1 checklist: [docs/phases/phase-1-foundation.md](docs/phases/phase-1-foundation.md)
- Local API: [docs/api/openapi-desktop-agent-v1.yaml](docs/api/openapi-desktop-agent-v1.yaml)
- Security: [docs/SECURITY.md](docs/SECURITY.md)

## Phase 1 layout

```text
modules/writing-domain          # entities, ports (no frameworks)
modules/writing-application     # use cases, prompts, validation, metrics
modules/writing-adapter-ai      # offline + OpenAI-compatible + failover
modules/writing-adapter-rest    # localhost JDK HttpServer
apps/desktop-agent              # composition root
```

## Quick start

```bash
./gradlew.bat qualityCheck
./gradlew.bat :apps:desktop-agent:run
```

```bash
curl -s http://127.0.0.1:8787/v1/assist -H "Content-Type: application/json" -d "{\"text\":\"Hello team.\",\"action\":\"REWRITE\"}"
```

Cloud with offline failover:

```bash
set ADAWRITER_AI_PROVIDER=openai-compatible+offline
set ADAWRITER_AI_API_KEY=sk-...
./gradlew.bat :apps:desktop-agent:run
```

## Delivery phases

| Phase | Branch | Focus |
|---|---|---|
| 1 | `feature/phase-1-foundation` | Foundation (current) |
| 2 | `feature/phase-2-text-detection` | Text detection & privacy |
| 3 | `feature/phase-3-ai-engine` | AI engine depth |
| 4 | `feature/phase-4-desktop-integration` | Desktop integration |
| 5 | `feature/phase-5-mobile-keyboard` | Mobile keyboard |
