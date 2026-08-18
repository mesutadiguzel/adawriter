# Security notes (Phase 1)

## Local agent

- Binds to `127.0.0.1` only
- No authentication yet (localhost trust boundary); do not expose the port remotely
- Request body size is capped
- Error responses avoid leaking provider internals for 5xx paths
- Secrets via environment variables only — never commit API keys

## AI providers

- Domain code never imports vendor SDKs
- Prefer `stub` offline; use `openai-compatible+stub` for failover during development
- Prompt catalog instructs the model to ignore instructions embedded in document content

## Reporting

If you discover a vulnerability, open a private security advisory on the repository or contact the maintainers. Do not file public issues with exploit details.
