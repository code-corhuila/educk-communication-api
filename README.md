# educk-communication-api

> communication bounded context: service API

Part of the **LMS Library** distributed system — team `lms-library`, Grupo 2.
Governance and documentation live in [`library-docs`](https://github.com/code-corhuila/library-docs).

## Branching

Three permanent branches. **None of them accepts a direct commit** — you enter through a child
branch and leave through a Pull Request.

```
develop  <--PR--  feat/... fix/... chore/...
qa       <--PR--  qa/...
main     <--PR--  release/...  hotfix/...
```

Promotion happens **by re-application** (`git cherry-pick -x`), never by merging one permanent
branch into another: `merge develop -> qa` and `merge qa -> main` do not exist in this model.

`main` requires **1 approval from `ariel5253`**. On `develop` and `qa` the team sets its own review
rule.

Full policy: `00-governance/branching-policy.md` in `library-docs`.

## Token verification (RS256)

Every route under `/api/` (except `/api/v1/messages/health`) requires an `RS256` bearer token issued by `educk-identity-api`.
Configure `JWT_PUBLIC_KEY` (X.509 SPKI PEM) in the environment; the service fails fast on startup if missing or invalid.
Message `senderId` is always bound to the verified token `sub` UUID rather than trusting request payloads.
