-- Outbox for enable og disable av microfrontenden syfo-dialog via Budstikka.
-- event_id lagres før første sending og gjenbrukes ved retry, slik at Budstikka kan deduplisere.
CREATE TABLE BUDSTIKKA_MIKROFRONTEND_UTSENDING (
    id                  UUID         PRIMARY KEY,
    event_id            UUID         NOT NULL,
    fnr                 VARCHAR(11)  NOT NULL,
    kommando            VARCHAR(20)  NOT NULL,                   -- 'ENABLE' | 'DISABLE'
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING', -- 'PENDING' | 'SENT' | 'SKIPPED'
    retry_count         INT          NOT NULL DEFAULT 0,
    next_retry_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMP    NOT NULL DEFAULT clock_timestamp(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_bmu_event_id UNIQUE (event_id)
);

-- Scheduler: ventende rader i rekkefølge, og eldre ventende rader for samme person
CREATE INDEX idx_bmu_pending_created ON BUDSTIKKA_MIKROFRONTEND_UTSENDING (created_at) WHERE status = 'PENDING';
CREATE INDEX idx_bmu_fnr_pending ON BUDSTIKKA_MIKROFRONTEND_UTSENDING (fnr, created_at) WHERE status = 'PENDING';

-- Opprydding
CREATE INDEX idx_bmu_created ON BUDSTIKKA_MIKROFRONTEND_UTSENDING (created_at);
