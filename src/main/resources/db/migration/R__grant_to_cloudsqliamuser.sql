REVOKE ALL ON ALL TABLES IN SCHEMA public FROM cloudsqliamuser;
REVOKE ALL ON ALL TABLES IN SCHEMA public FROM "disykefravar-x4wt@knada-gcp.iam";
REVOKE ALL ON ALL TABLES IN SCHEMA public FROM "disykefravar-x4wt@knada-gcp.iam.gserviceaccount.com";

GRANT SELECT ON ALL TABLES IN SCHEMA public TO cloudsqliamuser;

GRANT SELECT(
    id,
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
    opprettet_av_fnr)
ON motebehov to "disykefravar-x4wt@knada-gcp.iam.gserviceaccount.com";

GRANT SELECT ON MOTEBEHOV_DVH TO "disykefravar-x4wt@knada-gcp.iam.gserviceaccount.com";
