ALTER TABLE MOTEBEHOV
    DROP COLUMN IF EXISTS innmelder_type;

ALTER TABLE MOTEBEHOV
    ADD COLUMN innmelder_type VARCHAR(12) CHECK (innmelder_type IN ('ARBEIDSGIVER', 'ARBEIDSTAKER'));
