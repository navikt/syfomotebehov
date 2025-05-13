-- This update to 1M+ rows was run manually in smaller chunks to not run out of memory
UPDATE MOTEBEHOV
SET innmelder_type =
    CASE
        WHEN opprettet_av = aktoer_id OR opprettet_av_fnr = sm_fnr
            THEN 'ARBEIDSTAKER'
        ELSE 'ARBEIDSGIVER'
        END
WHERE id BETWEEN 0 AND 10000;
