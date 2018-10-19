-- ROLLBACK-START
------------------
-- DROP TABLE MOTEBEHOV;

---------------
-- ROLLBACK-END

CREATE TABLE MOTEBEHOV (
  motebehov_uuid              VARCHAR(36) NOT NULL,
  opprettet_dato              TIMESTAMP NOT NULL,
  opprettet_av                VARCHAR(13) NOT NULL,
  aktoer_id                   VARCHAR(13) NOT NULL,
  virksomhetsnummer           VARCHAR(9)  NOT NULL,
  friskmelding_forventning    CLOB,
  tiltak                      CLOB,
  tiltak_resultat             CLOB,
  har_motebehov               NUMBER CHECK (har_motebehov IN (1, 0)),
  forklaring                  CLOB,
  CONSTRAINT MOTEBEHOV_PK PRIMARY KEY (motebehov_uuid)
);
