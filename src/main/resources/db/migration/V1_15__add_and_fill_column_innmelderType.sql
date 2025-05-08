ALTER TABLE MOTEBEHOV
    DROP COLUMN IF EXISTS innmelder_type;

ALTER TABLE MOTEBEHOV
    ADD COLUMN innmelder_type VARCHAR(12) CHECK (innmelder_type IN ('ARBEIDSGIVER', 'ARBEIDSTAKER'));

UPDATE MOTEBEHOV
SET innmelder_type =
    CASE
        WHEN opprettet_av = aktoer_id OR opprettet_av_fnr = sm_fnr
            THEN 'ARBEIDSTAKER'
        ELSE 'ARBEIDSGIVER'
        END;

ALTER TABLE MOTEBEHOV
    ALTER COLUMN innmelder_type SET NOT NULL;