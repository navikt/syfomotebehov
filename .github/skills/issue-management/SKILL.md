---
description: Create and manage GitHub Issues linked to the Team eSyfo GitHub Projects board
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

### 3. Opprett issue med standardisert beskrivelse

#### Feature / Story / Task

~~~markdown
## Beskrivelse

[Kort og presis beskrivelse av hva som skal gjøres]

## Bakgrunn

[Hvorfor er dette nødvendig? Kontekst og motivasjon]

## Avhengigheter

[Valgfritt: Andre issues som må være løst først, f.eks. "Avhenger av #101"]
Del av epic: [#EPIC_NUMMER hvis relevant]

## Akseptansekriterier

- [ ] [Kriterium 1]
- [ ] [Kriterium 2]
- [ ] [Kriterium 3]

## Teknisk kontekst

[Valgfritt: Relevante filer, API-er, avhengigheter, lenker]
~~~

#### Bug

~~~markdown
## Beskrivelse

[Kort beskrivelse av feilen]

## Steg for å reprodusere

1. [Steg 1]
2. [Steg 2]

## Forventet oppførsel

[Hva burde skje]

## Faktisk oppførsel

[Hva skjer i dag]

## Teknisk kontekst

[Stacktrace, logger, relevante filer]
~~~

#### Epic

~~~markdown
## Mål

[Overordnet mål for epicen]

## Bakgrunn

[Kontekst og motivasjon]

## Omfang

[Hva er inkludert og hva er utenfor scope]

## Deloppgaver

Løses i rekkefølge. Issues uten innbyrdes avhengigheter kan løses parallelt.

- [ ] #NNN — [Kort beskrivelse]
- [ ] #NNN — [Kort beskrivelse] (avhenger av #NNN)
- [ ] #NNN — [Kort beskrivelse] (avhenger av #NNN, #NNN)

Oppdateres etterhvert som issues opprettes og fullføres.
~~~

### 4. Opprett issue med `gh` CLI

```bash
gh issue create \
  --repo navikt/REPO_NAVN \
  --title "Kort, beskrivende tittel" \
  --body "BODY_FRA_MAL_OVER" \
  --project "Team eSyfo"
```

Issuet legges automatisk til Team eSyfo-prosjektet via `--project`-flagget.

### 5. Sett project-felter

Etter opprettelse, sett riktig type og status i prosjektet:

```bash
# Finn prosjektnummer
gh project list --owner navikt --format json --jq '.projects[] | select(.title == "Team eSyfo") | .number'

# Sett type og status (krever project item ID)
# Bruk gh project item-list og item-edit for å oppdatere felter
```

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
3. Inkluder `Del av epic: #EPIC_NUMMER` i hvert underliggende issue
4. Inkluder avhengigheter: `Avhenger av #NNN` der det er relevant
5. Oppdater epicens deloppgave-liste med lenker til nye issues
6. Sett epicen til **Monday epics 🎯** hvis den skal jobbes med nå
7. Underliggende issues starter i **Backlog** eller **Plukk meg! 🙌**

#### Sub-issues skal være selvstendige

Hvert sub-issue skal inneholde nok kontekst til at noen kan plukke det opp uten å lese hele epicen:
- Tydelig beskrivelse av hva som skal gjøres
- Relevante filer og API-er (fra souschef-planen)
- Avhengigheter til andre issues
- Akseptansekriterier

Dette gjør at oppgaver kan gjenopptas neste dag eller overtas av et annet teammedlem uten re-analyse.

### 7. Stegvis løsning av epic

Når en epic skal løses stegvis:

1. **Les epicen** — Hent epic og alle sub-issues:
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
# Sjekk om åpne sub-issues gjenstår
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

## Sjekkliste

- [ ] Issue opprettet med standardisert beskrivelse
- [ ] Lagt til i Team eSyfo-prosjektet
- [ ] Riktig type satt
- [ ] Status satt (default: Backlog)
- [ ] Avhengigheter dokumentert (hvis del av epic)
- [ ] Sub-issues er selvstendige (nok kontekst til å jobbe uten epic-lesing)
- [ ] PR knyttet til issue (når arbeidet er ferdig)
- [ ] Completion comment lagt igjen etter løsning
- [ ] Epic-kobling satt opp (hvis relevant)
- [ ] Epic lukket når alle sub-issues er done
