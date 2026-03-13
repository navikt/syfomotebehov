# Dialogmøtekandidat – varsel-flyt

Denne siden dokumenterer den komplette flyten fra en kandidat-melding ankommer på Kafka til varsel er sendt via **esyfovarsel**, inkludert feilhåndtering, metrikker og logging.

---

## 1. Innledning

### Bakgrunn – outbox-mønsteret

Før outbox-mønsteret ble innført ble varsel sendt direkte fra `DialogmotekandidatService` i én og samme transaksjon som kandidatoppdateringen. Det medførte to problemer:

1. **Partial failure**: Hvis kallet til esyfovarsel feilet etter at databaseraden var persistert, gikk varselet tapt uten mulighet for retry.
2. **Kafka-retries blokkert**: Fordi feil i API-kallet kastet unntak oppover, ble ikke Kafka-offsetten bekreftet, noe som låste lytteren og trigget uendelige retries på *samme* melding.

Med outbox-mønsteret splittes ansvaret i to separate steg:

| Steg | Komponent | Ansvar |
|---|---|---|
| **Motta og lagre** | `DialogmotekandidatListener` + `DialogmotekandidatService` | Persisterer kandidatstatus og setter inn en `PENDING`-rad i `DIALOGKANDIDAT_VARSEL_STATUS`. Bekrefter Kafka-offset. |
| **Sende varsel** | `DialogmotekandidatVarselScheduler` | Poller `PENDING`-rader hvert minutt og kaller esyfovarsel. Har innebygd retry-teller. |
| **Rydde opp** | `DialogmotekandidatVarselScheduler.runCleanUp()` | Sletter gamle `SENT`- og `PENDING`-rader én gang i timen. |

---

## 2. Full flyt – sekvensdiagram

```mermaid
sequenceDiagram
    participant K as Kafka<br/>(isdialogmotekandidat)
    participant L as DialogmotekandidatListener
    participant S as DialogmotekandidatService<br/>[@Transactional]
    participant DB_K as DB: DIALOGMOTEKANDIDAT
    participant DB_V as DB: DIALOGKANDIDAT_VARSEL_STATUS
    participant Sched as DialogmotekandidatVarselScheduler<br/>[hvert minutt, leader only]
    participant VS as VarselServiceV2
    participant EP as EsyfovarselProducer
    participant KB as Kafka<br/>(team-esyfo.varselbus)

    K->>L: ConsumerRecord<KafkaDialogmotekandidatEndring>
    L->>S: receiveDialogmotekandidatEndring(melding)
    S->>DB_K: upsert kandidatstatus
    S->>DB_V: INSERT PENDING (VARSEL eller FERDIGSTILL)
    S-->>L: OK
    L->>K: acknowledgment.acknowledge()

    loop Hvert minutt
        Sched->>DB_V: hent PENDING VARSEL-rader
        Sched->>VS: sendSvarBehovVarsel(fnr, uuid)
        VS->>EP: produserVarsel(SM + NL)
        EP->>KB: produce melding
        Sched->>DB_V: updateStatusToSent

        Sched->>DB_V: hent PENDING FERDIGSTILL-rader
        Sched->>VS: ferdigstillSvarMotebehovVarsel(fnr)
        VS->>EP: produserFerdigstill(SM + NL)
        EP->>KB: produce melding
        Sched->>DB_V: updateStatusToSent
    end
```

---

## 3. Beslutningslogikk – flowchart

Logikken i `receiveDialogmotekandidatEndring` avgjør hvilken `PENDING`-rad som settes inn (eller om meldingen ignoreres).

```mermaid
flowchart TD
    A([Ny KafkaDialogmotekandidatEndring]) --> B{Er DB-rad nyere\nenn meldingen?}
    B -- Ja --> IG1[IGNORE\nreason: newer_change_exists]
    B -- Nei --> C[Upsert DIALOGMOTEKANDIDAT]
    C --> D{kandidat=true?}

    D -- Nei --> FE[INSERT PENDING\nFERDIGSTILL]
    D -- Ja --> E{Eksisterende rad\ni DB?}

    E -- Nei --> VA1[INSERT PENDING\nVARSEL]
    E -- Ja --> F{DB.kandidat=true?}

    F -- Ja --> IG2[IGNORE\nreason: already_kandidat]
    F -- Nei --> VA2[INSERT PENDING\nVARSEL]
```

### Beslutningstabell

| Melding: `kandidat` | DB: eksisterende rad | Resultat |
|---|---|---|
| *(DB er nyere)* | — | **IGNORE** (`newer_change_exists`) |
| `true` | ingen rad | **VARSEL** |
| `true` | `kandidat=true` | **IGNORE** (`already_kandidat`) |
| `true` | `kandidat=false` | **VARSEL** |
| `false` | ingen rad | **FERDIGSTILL** |
| `false` | `kandidat=false` | **FERDIGSTILL** |
| `false` | `kandidat=true` | **FERDIGSTILL** |

---

## 4. Esyfovarsel-utsendelse – sekvensdiagram

```mermaid
sequenceDiagram
    participant Sched as DialogmotekandidatVarselScheduler
    participant VS as VarselServiceV2
    participant OT as OppfolgingstilfelleConsumer
    participant DM as DialogmoteConsumer
    participant NL as NarmesteLederConsumer
    participant EP as EsyfovarselProducer
    participant KB as Kafka<br/>(team-esyfo.varselbus)

    Sched->>VS: sendSvarBehovVarsel(fnr, uuid)
    VS->>OT: hent aktivt oppfølgingstilfelle
    OT-->>VS: tilfelle (eller absent)
    VS->>DM: er dialogmøte allerede planlagt?
    DM-->>VS: ja / nei
    VS->>NL: hent nærmeste leder-relasjoner
    NL-->>VS: liste med ledere

    VS->>EP: send SM_DIALOGMOTE_SVAR_MOTEBEHOV (arbeidstaker)
    EP->>KB: produce varsel

    loop for hver nærmeste leder
        VS->>EP: send NL_DIALOGMOTE_SVAR_MOTEBEHOV (leder)
        EP->>KB: produce varsel
    end

    VS-->>Sched: OK
```

**`ferdigstillSvarMotebehovVarsel(fnr)`** følger tilsvarende mønster, men hopper over tilfelle/møtesjekk og lukker varsel for arbeidstaker og alle nærmeste ledere.

---

## 5. Opprydding – sekvensdiagram

```mermaid
sequenceDiagram
    participant Sched as DialogmotekandidatVarselScheduler<br/>[hvert 60 minutt, leader only]
    participant DB as DB: DIALOGKANDIDAT_VARSEL_STATUS

    Sched->>DB: deleteSentOlderThan(1 måned)
    DB-->>Sched: antall slettet (sentDeleted)

    Sched->>DB: deletePendingOlderThan(2 uker)
    DB-->>Sched: antall slettet (pendingDeleted)

    Sched->>Sched: log event dialogmotekandidat.cleanup\n{sentDeleted, pendingDeleted}
```

`PENDING`-rader eldre enn 2 uker regnes som ikke-leverbare og fjernes for å unngå evig retry.

---

## 6. Strukturert logging

Alle log-hendelser bruker `net.logstash.logback.argument.StructuredArguments.kv` og produserer JSON-logger som kan søkes i Grafana Loki.

| Event | Komponent | Beskrivelse | Felter |
|---|---|---|---|
| `dialogmotekandidat.received` | `DialogmotekandidatListener` | Ny Kafka-melding mottatt | `event`, `topic`, `uuid` |
| `dialogmotekandidat.ignored` | `DialogmotekandidatService` | Melding ignorert | `event`, `reason` (`newer_change_exists` / `already_kandidat`), `messageId` |
| `dialogmotekandidat.created` | `DialogmotekandidatService` | Ny kandidatrad opprettet | `event`, `messageId` |
| `dialogmotekandidat.updated` | `DialogmotekandidatService` | Eksisterende kandidat oppdatert | `event`, `messageId` |
| `dialogmotekandidat.varsel.sent` | `DialogmotekandidatVarselScheduler` | Varsel sendt OK | `event`, `id`, `messageId` |
| `dialogmotekandidat.varsel.retry` | `DialogmotekandidatVarselScheduler` | Varsel feilet, teller økt | `event`, `id`, `messageId`, `retryCount` |
| `dialogmotekandidat.ferdigstill.sent` | `DialogmotekandidatVarselScheduler` | Ferdigstilling sendt OK | `event`, `id`, `messageId` |
| `dialogmotekandidat.ferdigstill.retry` | `DialogmotekandidatVarselScheduler` | Ferdigstilling feilet, teller økt | `event`, `id`, `messageId`, `retryCount` |
| `dialogmotekandidat.cleanup` | `DialogmotekandidatVarselScheduler` | Opprydding gjennomført | `event`, `sentDeleted`, `pendingDeleted` |

### Loki-eksempel – spore én melding gjennom retry-løkken

```logql
{app="syfomotebehov"} | json | event="dialogmotekandidat.varsel.retry" | messageId="<uuid>"
```

Bytt ut `<uuid>` med `uuid`-verdien fra den opprinnelige `dialogmotekandidat.received`-hendelsen.

---

## 7. Prometheus-metrikker

Scheduleren eksponerer to gauges som måler «stuck» rader – `PENDING`-rader som har ligget usendt i mer enn én dag.

| Metrikknavn | Tag | Beskrivelse |
|---|---|---|
| `dialogkandidat_varsel_pending_over_1d_total` | `type=VARSEL` | Antall VARSEL-rader som har vært `PENDING` i mer enn 1 dag |
| `dialogkandidat_varsel_pending_over_1d_total` | `type=FERDIGSTILL` | Antall FERDIGSTILL-rader som har vært `PENDING` i mer enn 1 dag |

### PromQL-eksempel

```promql
dialogkandidat_varsel_pending_over_1d_total{app="syfomotebehov", type="VARSEL"} > 0
```

### Anbefalt alert

Trigger hvis verdien er `> 0` i mer enn **30 minutter**. Det indikerer at scheduleren ikke klarer å levere varslene, og krever manuell undersøkelse (sjekk esyfovarsel-connectivitet og retry-tellere i Loki).
