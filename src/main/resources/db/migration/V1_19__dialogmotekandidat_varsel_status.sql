CREATE TABLE DIALOGKANDIDAT_VARSEL_STATUS (
    id                  UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    kafka_melding_uuid  VARCHAR(36)  NOT NULL,
    fnr                 VARCHAR(11)  NOT NULL,
    type                VARCHAR(20)  NOT NULL,                   -- 'VARSEL' | 'FERDIGSTILL'
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING', -- 'PENDING' | 'SENT'
    retry_count         INT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_kafka_melding_uuid UNIQUE (kafka_melding_uuid)
);

-- Scheduler fase 1 og 2: type + status + created_at
CREATE INDEX idx_dvs_type_status_created ON DIALOGKANDIDAT_VARSEL_STATUS (type, status, created_at);

-- Dedikert cleanup-indeks for SENT-rader (partial index)
CREATE INDEX idx_dvs_sent_cleanup ON DIALOGKANDIDAT_VARSEL_STATUS (updated_at) WHERE status = 'SENT';

-- Dedikert cleanup-indeks for ekspirerte PENDING-rader (partial index)
CREATE INDEX idx_dvs_pending_cleanup ON DIALOGKANDIDAT_VARSEL_STATUS (created_at) WHERE status = 'PENDING';

-- drifts/debug-indeks for oppslag per person
CREATE INDEX idx_dvs_fnr ON DIALOGKANDIDAT_VARSEL_STATUS (fnr);
