# AdaWriter

Privacy-first AI writing assistant focused on **desktop** first (mobile deferred).

## Engineering standards

Every decision ranks: **UX → Performance → Security → Simplicity → Reliability → Maintainability → Scalability → Cost**.

- Requirements: [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md)
- Delivery process: [docs/DELIVERY.md](docs/DELIVERY.md)
- Autonomous mode: [docs/AUTONOMOUS_DELIVERY.md](docs/AUTONOMOUS_DELIVERY.md)
- Architecture ADRs: [docs/architecture/](docs/architecture/)
- Local API: [docs/api/openapi-desktop-agent-v1.yaml](docs/api/openapi-desktop-agent-v1.yaml)
- Security: [docs/SECURITY.md](docs/SECURITY.md)

## Desktop layout (active focus)

```text
modules/writing-* + privacy-*   # hexagonal core
apps/desktop-agent              # localhost API only
apps/desktop-shell              # tray + clipboard assist + API
```

## Quick start (desktop)

```bash
./gradlew.bat desktopQualityCheck
./gradlew.bat :apps:desktop-shell:run
```

Tray actions: rewrite / shorten / expand / fix grammar on clipboard text.  
API: `http://127.0.0.1:8787` (`/v1/assist`, `/v1/privacy/detect`, `/v1/privacy/redact`)

```bash
curl -s http://127.0.0.1:8787/v1/assist -H "Content-Type: application/json" -d "{\"text\":\"Hello team.\",\"action\":\"REWRITE\"}"
curl -s http://127.0.0.1:8787/v1/privacy/detect -H "Content-Type: application/json" -d "{\"text\":\"Reach jane@example.com\"}"
```

## Testing

**Rule:** aim for **100% JaCoCo line coverage** when the application is finished. Phase work may land below that temporarily; gaps must be closed before done.

```bash
# Desktop modules only (recommended)
./gradlew.bat desktopQualityCheck

# Alias
./gradlew.bat qualityCheck
```

Positive and negative JUnit coverage covers domain validation, privacy detection/redaction, assist use-case, REST API, AI routing/circuit breaker, and desktop clipboard assist.
