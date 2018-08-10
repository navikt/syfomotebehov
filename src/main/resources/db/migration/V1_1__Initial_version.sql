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
  friskmelding_forventning    VARCHAR(MAX),
  tiltak                      VARCHAR(MAX),
  tiltak_resultat             VARCHAR(MAX),
  har_motebehov               BOOLEAN,
  forklaring                  VARCHAR(MAX)
);
