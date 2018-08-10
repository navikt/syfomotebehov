-- ROLLBACK-START
------------------
-- DROP TABLE MOTEBEHOV;

---------------
-- ROLLBACK-END

CREATE TABLE MOTEBEHOV (
  motebehov_uuid              VARCHAR(36) NOT NULL PRIMARY KEY,
  opprettet_dato              TIMESTAMP NOT NULL,
  opprettet_av                VARCHAR(13) NOT NULL,
  aktoer_id                   VARCHAR(13) NOT NULL,
  virksomhetsnummer           VARCHAR(9)  NOT NULL,
  friskmelding_forventning    TEXT,
  tiltak                      TEXT,
  tiltak_resultat             TEXT,
  har_motebehov               BOOLEAN,
  forklaring                  TEXT
);
