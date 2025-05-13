-- This update to 1M+ rows was run manually on a limited number of rows at a time to not run out of memory
UPDATE MOTEBEHOV
SET innmelder_type =
    CASE
        WHEN opprettet_av = aktoer_id OR opprettet_av_fnr = sm_fnr
            THEN 'ARBEIDSTAKER'
        ELSE 'ARBEIDSGIVER'
        END
WHERE innmelder_type IS NULL
--   AND id BETWEEN 0 AND 100000;
