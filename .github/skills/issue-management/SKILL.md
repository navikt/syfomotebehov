---
description: Opprett og administrer GitHub Issues, epics, sub-issues og prosjektstatus på Team eSyfos GitHub Projects-board
---
<!-- Managed by esyfo-cli. Do not edit manually. Changes will be overwritten.
     For repo-specific customizations, create your own files without this header. -->

# Issue Management — Team eSyfo

Opprett og håndter GitHub Issues knyttet til Team eSyfo sitt GitHub Projects-board i `navikt`-organisasjonen.

## Workflow

### 1. Sjekk om issue allerede finnes

Før du oppretter et nytt issue, sjekk om brukeren allerede har referert til et issue (f.eks. `#123` eller en GitHub-URL). Hvis ja, bruk det eksisterende issuet.

### 2. Velg type

| Type | Bruk |
|------|------|
| **Epic** | Store oppgaver som brytes ned i flere issues |
| **Feature** | Ny funksjonalitet |
| **Story** | Brukerhistorie / use case |
| **Task** | Teknisk oppgave, vedlikehold, chore |
| **Bug** | Feil som må fikses |

### 3. Opprett issue med riktig struktur

Repoet har issue-templates i `.github/ISSUE_TEMPLATE/` for hvert type (feature, bug, task, epic). Les feltstrukturen fra templaten for den valgte typen og lag en markdown-body med tilsvarende seksjoner.

Inkluder alltid:
- **Avhengigheter** (valgfritt): `Avhenger av #NNN` hvis relevant
- **Epic-kobling** (valgfritt): `Del av epic: #EPIC_NUMMER` hvis relevant

### 4. Opprett issue

**MCP (foretrukket):** Bruk `issue_write`-verktøyet med `type`-parameter for å opprette issue med riktig type direkte.

**Fallback (`gh api`):**
```bash
gh api repos/navikt/REPO_NAVN/issues \
  -X POST \
  -f title="Kort, beskrivende tittel" \
  -f body="BODY" \
  -f type="Feature" \
  --jq '.html_url'
```

Legg deretter til i Team eSyfo-prosjektet (bruk `html_url` fra forrige steg):
```bash
gh project item-add PROJECT_NUMBER --owner navikt --url ISSUE_URL --format json
```

Se `references/projects.md` for prosjektnummer og feltoppdatering.

Se `references/issue-types.md` for detaljer om issue types.

### 5. Sett project-felter

Issue-templates har `projects: ["navikt/157"]` som automatisk legger issues i Team eSyfo-prosjektet når de opprettes via web UI. Ved programmatisk opprettelse (`gh api`, `gh issue create`, MCP), legg til manuelt:

**MCP (foretrukket):** Bruk `projects_write`-verktøyet for å legge issue i prosjektet og sette Status og Type.

**`gh issue create`:** Bruk `--project "Team eSyfo"` for å legge til i prosjektet automatisk.

**Fallback (`gh project`):** Se `references/projects.md` for komplett workflow med `gh project item-add` og `gh project item-edit`.

**Statuser:**
| Status | Bruk |
|--------|------|
| **Backlog** | Nyopprettede issues (default) |
| **Plukk meg! 🙌** | Klar til å plukkes opp |
| **Jeg jobbes med! ⚒️** | Under arbeid |
| **Monday epics 🎯** | Epics som er aktive i nåværende sprint |
| **Done** | Ferdig |

**Typer:** Bug, Epic, Feature, Story, Task

### 6. Epic-håndtering

For store oppgaver som brytes ned:

1. Opprett epic-issuet først (type: Epic)
2. Opprett underliggende issues (type: Task/Story/Feature)
3. Koble sub-issues til epicen via native sub-issues API:
   - **MCP (foretrukket):** Bruk `sub_issue_write` med action `add` for å legge til sub-issue
   - **Fallback:** Se `references/sub-issues.md` for `gh api` REST-kommandoer
   - Inkluder også `Del av epic: #EPIC_NUMMER` i issue-body for lesbarhet
4. Koble avhengigheter via native dependencies API:
   - Se `references/dependencies.md` for MCP og `gh api`-kommandoer
   - Inkluder også `Avhenger av #NNN` i issue-body for lesbarhet
5. Oppdater epicens deloppgave-liste med lenker til nye issues
6. Sett epicen til **Monday epics 🎯** hvis den skal jobbes med nå
7. Underliggende issues starter i **Backlog** eller **Plukk meg! 🙌**

#### Sub-issues skal være selvstendige

Hvert sub-issue skal inneholde nok kontekst til at noen kan plukke det opp uten å lese hele epicen:
- Tydelig beskrivelse av hva som skal gjøres
- Relevante filer og API-er (fra souschef-planen)
- Avhengigheter til andre issues
- Akseptansekriterier

### 7. Stegvis løsning av epic

Når en epic skal løses stegvis:

1. **Les epicen** — Hent epic og alle sub-issues:
   **MCP (foretrukket):** Bruk `issue_read` for å lese epicen, og list sub-issues via MCP.

   **Fallback:**
   ```bash
   gh issue view EPIC_NUMMER --repo navikt/REPO
   gh issue list --repo navikt/REPO --search "Del av epic: #EPIC_NUMMER" --state all --json number,title,state
   ```

2. **Finn neste oppgave** — Identifiser issues der:
   - Issuet er åpent (ikke lukket)
   - Alle avhengigheter er oppfylt (avhengige issues er lukket)
   - Hvis flere kandidater: velg den med lavest nummer (følger planlagt rekkefølge)

3. **Foreslå neste** — Presenter til brukeren:
   > *"Epic #120 har 3/8 oppgaver fullført. Neste er #124: [tittel]. Avhengigheter oppfylt. Skal jeg starte?"*

4. **Løs oppgaven** — Følg normal arbeidsflyt for det valgte issuet

5. **Gjenta** — Etter fullføring, sjekk om epicen er ferdig (se seksjon 9)

### 8. Completion comments

Etter at et issue er løst, legg igjen en kommentar på issuet:

```bash
gh issue comment ISSUE_NUMMER --repo navikt/REPO --body "COMMENT_BODY"
```

Kommentaren skal inneholde:

~~~markdown
## ✅ Løst

**Oppsummering:** [Kort beskrivelse av hva som ble gjort]

**Endrede filer:**
- `src/path/to/file1.ts` — [hva som ble endret]
- `src/path/to/file2.ts` — [hva som ble endret]

**PR:** #PR_NUMMER

**Mattilsynsrapport:** [Smilefjes] — [Kort oppsummering av eventuelle funn]
~~~

### 9. Epic auto-close

Etter at et sub-issue er lukket, sjekk om alle sub-issues i epicen er fullført:

```bash
gh issue list --repo navikt/REPO --search "Del av epic: #EPIC_NUMMER" --state open --json number
```

Hvis ingen åpne sub-issues gjenstår:
1. Legg igjen en oppsummerende kommentar på epicen:
   ~~~markdown
   ## 🎉 Epic fullført

   Alle deloppgaver er løst.

   **Fullførte issues:**
   - #101 — [tittel]
   - #102 — [tittel]
   - #103 — [tittel]
   ~~~
2. Lukk epicen: `gh issue close EPIC_NUMMER --repo navikt/REPO`
3. Sett status til **Done** i prosjektet

### 10. Knytt PR til issue

Når arbeidet resulterer i en PR, knytt den til issuet:

```bash
# I PR-beskrivelsen eller commit-meldingen:
Closes #ISSUE_NUMMER

# Eller for delvis arbeid (holder issuet åpent):
Relates to #ISSUE_NUMMER
```

## Beslutningstre

```
Er oppgaven stor nok for en epic?
├── Ja → Opprett Epic + underliggende issues
│   ├── Skal jobbes med nå? → Sett epic til "Monday epics 🎯"
│   └── Hvert sub-issue: selvstendige med avhengigheter og kontekst
└── Nei → Opprett frittstående issue
    └── Type? → Feature / Story / Task / Bug
```
