CREATE TABLE VARSEL_OUTBOX
(
    uuid       UUID         NOT NULL PRIMARY KEY,
    kilde      VARCHAR(100) NOT NULL,
    payload    JSONB        NOT NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL
);
CREATE INDEX idx_varsel_outbox_status ON VARSEL_OUTBOX (status);

CREATE TABLE VARSEL_OUTBOX_RECIPIENT
(
    uuid         UUID        NOT NULL PRIMARY KEY,
    outbox_uuid  UUID REFERENCES VARSEL_OUTBOX (uuid),
    mottaker_fnr VARCHAR(11) NOT NULL,
    payload      JSONB       NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at   TIMESTAMP   NOT NULL,
    updated_at   TIMESTAMP   NOT NULL
);
CREATE UNIQUE INDEX idx_varsel_outbox_recipient_dedup
    ON VARSEL_OUTBOX_RECIPIENT (outbox_uuid, mottaker_fnr)
    WHERE outbox_uuid IS NOT NULL;
