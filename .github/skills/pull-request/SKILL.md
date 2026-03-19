---
description: Opprett pull requests med semantisk tittel, issue-kobling og sjekkliste etter Team eSyfos arbeidsflyt
---
<!-- Managed by esyfo-cli. Do not edit manually. Changes will be overwritten.
     For repo-specific customizations, create your own files without this header. -->

# Pull Request — Team eSyfo

Opprett konsistente, velstrukturerte pull requests som kobles til issues og følger teamets arbeidsflyt.

## PR-tittel

Bruk semantisk commit-format:

```
type(scope): kort beskrivelse
```

- **Typer:** `feat`, `fix`, `refactor`, `chore`, `docs`, `test`, `style`
- **Scope:** Modul eller domene som endres (f.eks. `auth`, `api`, `sykmelding`)
- **Eksempler:**
  - `feat(oppfolgingsplan): add approval workflow`
  - `fix(api): handle null response from syfoperson`
  - `refactor(db): simplify migration rollback logic`

## PR-body

Repoet har en PR-template i `.github/PULL_REQUEST_TEMPLATE.md` som automatisk pre-fyller body når du oppretter en PR. Fyll inn seksjonene i templaten.

## Issue-kobling

| Situasjon | I PR-body |
|-----------|-----------|
| Issue løst fullstendig | `Closes #123` |
| Delvis arbeid, issue fortsatt åpent | `Relates to #123` |
| Del av epic | `Closes #123` + `Del av epic: #100` |
| Ingen issue | Skriv motivasjon direkte i beskrivelsen |

## Opprettelse

### MCP (foretrukket)

Bruk `create_pull_request` MCP-verktøyet. Fyll inn title og body.

### Fallback (gh CLI)

```bash
gh pr create \
  --repo navikt/REPO_NAVN \
  --title "type(scope): beskrivelse" \
  --body "BODY"
```

### Auto-merge (squash)

Etter opprettelse:
```bash
gh pr merge --auto --squash
```
