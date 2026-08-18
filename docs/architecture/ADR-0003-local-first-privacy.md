# ADR 0003: Local-First Privacy & Secure Defaults

## Status

Accepted

## Context

Privacy is a core feature. Users must trust continuous desktop/browser operation.

## Decision

1. **Local-first**: Prefer on-device processing; send data off-device only with explicit purpose and consent.
2. **Data minimization**: Store only what a feature requires; retention is configurable and deletable by the user.
3. **Sensitive content**: Detect and redact/block sensitive data before network egress.
4. **Zero Trust**: Authenticate/authorize every privileged action; least privilege for tokens and agent permissions.
5. **Secrets**: Never commit secrets; use OS keychain / secret managers; encrypt at rest for stored credentials and synced payloads.
6. **Prompt injection defenses**: Treat untrusted document/page content as data, not instructions; validate model outputs before side effects.

## Consequences

- AI gateway must support local models and cloud fallbacks
- Observability must avoid logging raw sensitive payloads
- Some features degrade gracefully offline instead of failing closed on UX
