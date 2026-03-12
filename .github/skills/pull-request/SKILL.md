---
description: Create standardized pull requests linked to GitHub Issues and the Team eSyfo workflow
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

Bruk denne malen for PR-beskrivelsen:

~~~markdown
## Beskrivelse

[Kort beskrivelse av hva denne PR-en gjør og hvorfor]

## Endringer

- [Fil/modul]: [Hva som ble endret]
- [Fil/modul]: [Hva som ble endret]

## Issue

Closes #ISSUE_NUMMER

## Sjekkliste

- [ ] Koden kompilerer og linter uten feil
- [ ] Endringene er testet (manuelt eller automatisk)
- [ ] Ingen sensitiv data eksponert (tokens, credentials, PII)
~~~

### Varianter

**Uten issue** (frittstående arbeid):
Erstatt `Closes #ISSUE_NUMMER` med en kort forklaring av motivasjonen.

**Delvis arbeid** (issuet er ikke ferdig):
Bruk `Relates to #ISSUE_NUMMER` i stedet for `Closes`.

**Epic sub-issue**:
Legg til referanse til epicen:
~~~markdown
## Issue

Closes #SUB_ISSUE_NUMMER
Del av epic: #EPIC_NUMMER
~~~

## Opprettelse med `gh` CLI

```bash
gh pr create \
  --repo navikt/REPO_NAVN \
  --title "type(scope): beskrivelse" \
  --body "BODY_FRA_MAL_OVER"
```

### Med auto-merge (squash)

```bash
gh pr create \
  --repo navikt/REPO_NAVN \
  --title "type(scope): beskrivelse" \
  --body "BODY_FRA_MAL_OVER" \
  && gh pr merge --auto --squash
```

## Kobling til issues

| Situasjon | I PR-body |
|-----------|-----------|
| Issue løst fullstendig | `Closes #123` |
| Delvis arbeid, issue fortsatt åpent | `Relates to #123` |
| Del av epic | `Closes #123` + `Del av epic: #100` |
| Ingen issue | Skriv motivasjon direkte i beskrivelsen |

## Sjekkliste

- [ ] Tittel følger semantisk commit-format
- [ ] Beskrivelse forklarer hva og hvorfor
- [ ] Endrede filer listet opp
- [ ] Issue-kobling inkludert (hvis relevant)
- [ ] Ingen sensitiv data i PR-en
