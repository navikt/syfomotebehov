DROP TABLE IF EXISTS MOTEBEHOV;

CREATE TABLE MOTEBEHOV (
  id                          SERIAL PRIMARY KEY,
  motebehov_uuid              VARCHAR(36) NOT NULL UNIQUE,
  opprettet_dato              TIMESTAMP NOT NULL,
  opprettet_av                VARCHAR(13) NOT NULL,
  aktoer_id                   VARCHAR(13) NOT NULL,
  virksomhetsnummer           VARCHAR(9)  NOT NULL,
  har_motebehov               BOOLEAN NOT NULL,
  forklaring                  TEXT,
  tildelt_enhet               VARCHAR(10),
  behandlet_tidspunkt         TIMESTAMP,
  behandlet_veileder_ident    VARCHAR (20),
  skjematype                  VARCHAR(10),
  sm_fnr                      VARCHAR(11),
  opprettet_av_fnr            VARCHAR(11)
);

CREATE INDEX motebehov_opprettet_av_aktor_id_index ON MOTEBEHOV(opprettet_av, aktoer_id);

CREATE INDEX motebehov_aktor_id_index ON MOTEBEHOV(aktoer_id);

CREATE INDEX motebehov_sm_fnr_index ON MOTEBEHOV(sm_fnr);
