CREATE VIEW MOTEBEHOV_DVH (
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
  opprettet_av_fnr
) AS SELECT
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
    opprettet_av_fnr
FROM MOTEBEHOV;