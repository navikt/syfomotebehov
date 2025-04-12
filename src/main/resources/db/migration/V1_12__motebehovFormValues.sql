-- Create the motebehovFormValues table
CREATE TABLE MOTEBEHOV_FORM_VALUES
(
    motebehov_uuid                      VARCHAR(36) NOT NULL,
    form_snapshot                       JSONB       NOT NULL,
    -- The fields below are only used for debugging and data monitoring purposes
    form_identifier                     VARCHAR(50) NOT NULL CHECK (form_identifier IN (
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

CREATE INDEX idx_motebehov_form_values_motebehov_uuid ON MOTEBEHOV_FORM_VALUES (motebehov_uuid);

ALTER TABLE MOTEBEHOV_FORM_VALUES
    ADD CONSTRAINT fk_motebehov
        FOREIGN KEY (motebehov_uuid)
            REFERENCES MOTEBEHOV (motebehov_uuid)
            ON DELETE CASCADE;