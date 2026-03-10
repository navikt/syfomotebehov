# Transactional Outbox for Varselutsending (Issue #591)

## Problem

Tidligere flyt for `kandidat=true` Kafka-meldinger:

```
Kafka-melding → DB: lagre kandidatstatus → API-kall (narmesteLeder, osv.) → Kafka: send varsel → ACK
```

Dersom noe feilet **etter** DB-skriving men **før** ACK:
- Kafka re-leverte meldingen
- `isKandidatFromBefore = true` → varsel ble hoppet over
- Melding ACKes → varsel er **tapt**

## Løsning: Transactional Outbox Pattern

Flyten er splittet i to atomiske deler:

**Del 1 — I Kafka-listeneren (synkron, én transaksjon):**
```
Kafka-melding
    ↓
@Transactional {
    DB: lagre/oppdater kandidatstatus
    DB: lagre outbox-entry (status=PENDING)
}
    ↓ ACK  ← trygt: begge er skrevet eller ingen
```

**Del 2 — Asynk scheduler:**
```
@Scheduled(hvert minutt)
    ↓
DB: hent PENDING outbox-entries
    ↓ For hver entry:
    ├─ Eldre enn 7 dager? → SKIPPED (med logging/alert)
    ├─ Ikke lenger kandidat? → SKIPPED
    └─ Ekspander til mottakere → send varsel → SENT
```

## Databaseskjema

To nye tabeller (Flyway `V1_19__add_varsel_outbox.sql`):

```sql
CREATE TABLE VARSEL_OUTBOX (
    uuid        UUID         NOT NULL PRIMARY KEY,  -- fra Kafka-meldingens uuid
    kilde       VARCHAR(100) NOT NULL,              -- f.eks. 'DIALOGMOTEKANDIDAT_LISTENER'
    payload     JSONB        NOT NULL,              -- rå Kafka-melding
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',  -- PENDING | PROCESSED | SKIPPED
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL
);

CREATE TABLE VARSEL_OUTBOX_RECIPIENT (
    uuid            UUID         NOT NULL PRIMARY KEY,
    outbox_uuid     UUID         REFERENCES VARSEL_OUTBOX(uuid),  -- nullable
    mottaker_fnr    VARCHAR(11)  NOT NULL,
    payload         JSONB        NOT NULL,   -- serialisert EsyfovarselHendelse
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',  -- PENDING | SENT
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP    NOT NULL
);
-- Forhindrer doble mottaker-rader ved retry:
CREATE UNIQUE INDEX idx_outbox_recipient
    ON VARSEL_OUTBOX_RECIPIENT(outbox_uuid, mottaker_fnr)
    WHERE outbox_uuid IS NOT NULL;
```

## Schedulerflyt

```
Fase 1 — Ekspansjon (VARSEL_OUTBOX PENDING → PROCESSED/SKIPPED):
  For hvert entry:
  1. Eldre enn 7 dager?                              → SKIPPED
  2. kandidat=true og person ikke lenger kandidat?   → SKIPPED
  3. kandidat=false og person er kandidat igjen?     → SKIPPED
  4. Hent NL-liste → INSERT VARSEL_OUTBOX_RECIPIENT (ON CONFLICT DO NOTHING)
     → VARSEL_OUTBOX: PROCESSED

Fase 2 — Utsending (VARSEL_OUTBOX_RECIPIENT PENDING → SENT):
  For hvert entry:
  - Deserialiser payload → EsyfovarselHendelse → send til esyfovarsel-topic → SENT
  - Feil: logg + behold PENDING (automatisk retry neste kjøring)
```

## Nye filer

| Fil | Type | Beskrivelse |
|-----|------|-------------|
| `src/main/resources/db/migration/V1_19__add_varsel_outbox.sql` | NY | Flyway-migrasjon |
| `src/main/kotlin/.../database/VarselOutboxEntry.kt` | NY | Data classes + enums |
| `src/main/kotlin/.../database/VarselOutboxDao.kt` | NY | CRUD for VARSEL_OUTBOX |
| `src/main/kotlin/.../database/VarselOutboxRecipientDao.kt` | NY | CRUD for VARSEL_OUTBOX_RECIPIENT |
| `src/main/kotlin/.../scheduler/VarselOutboxScheduler.kt` | NY | To-fase scheduler |
| `src/test/kotlin/.../VarselOutboxDaoTest.kt` | NY | DAO-integrasjonstester |
| `src/test/kotlin/.../VarselOutboxRecipientDaoTest.kt` | NY | DAO-integrasjonstester |
| `src/test/kotlin/.../VarselOutboxSchedulerTest.kt` | NY | Scheduler-integrasjonstester |

## Endrede filer

| Fil | Endring |
|-----|---------|
| `DialogmotekandidatService.kt` | `@Transactional`, skriv til outbox i stedet for direkte varsel |
| `VarselServiceV2.kt` | Fjernet dead methods (kandidat-spesifikke) |
| `EsyfovarselService.kt` | Fjernet dead methods |
| `DialogmotekandidatServiceTest.kt` | Tilpasset ny design |
| `VarselServiceTest.kt` | Slettet (alle metoder fjernet) |

## Observabilitet

- Prometheus gauge `varsel_outbox_pending_stuck_count` — antall PENDING entries eldre enn 24 timer
- `WARN`-logg med UUID-liste ved stuck entries
- Max-alder for outbox: 7 dager (hardkodet konstant `MAX_AGE_DAYS`)
- Slack-alerting via NAIS alertmanager: tas ved et senere tidspunkt

## Avklarte beslutninger

- **7 dager som max-alder:** hardkodet, kan endres direkte i `VarselOutboxScheduler`
- **Ingen relevanssjekk i fase 2:** sannsynlighet for utdatert varsel ved Kafka-nedetid er lav; konsekvens lav
- **Slack-alert:** utenfor scope, tas separat
