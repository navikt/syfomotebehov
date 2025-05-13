DROP TABLE IF EXISTS MOTEBEHOV_FORM_VALUES;

CREATE TABLE MOTEBEHOV_FORM_VALUES
(
    motebehov_row_id                    INTEGER PRIMARY KEY,
    form_snapshot                       JSONB       NOT NULL,
    form_identifier                     VARCHAR(30) NOT NULL CHECK (form_identifier IN (
        'motebehov-arbeidsgiver-svar',
        'motebehov-arbeidsgiver-meld',
        'motebehov-arbeidstaker-svar',
        'motebehov-arbeidstaker-meld'
    )),
    form_semantic_version               VARCHAR(20) NOT NULL,
    begrunnelse                         TEXT,
    onsker_sykmelder_deltar             BOOLEAN     NOT NULL,
    onsker_sykmelder_deltar_begrunnelse TEXT,
    onsker_tolk                         BOOLEAN     NOT NULL,
    tolk_sprak                          TEXT
);

ALTER TABLE MOTEBEHOV_FORM_VALUES
    ADD CONSTRAINT fk_motebehov
        FOREIGN KEY (motebehov_row_id)
            REFERENCES MOTEBEHOV (id)
            ON DELETE CASCADE;