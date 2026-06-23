CREATE OR REPLACE VIEW MOTEBEHOV_DVH AS
    SELECT
        motebehov_uuid,
        opprettet_dato,
        opprettet_av,
        virksomhetsnummer,
        har_motebehov,
        tildelt_enhet,
        behandlet_tidspunkt,
        behandlet_veileder_ident,
        skjematype,
        sm_fnr,
        opprettet_av_fnr,
        innmelder_type
     FROM MOTEBEHOV;
