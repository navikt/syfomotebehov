-- ROLLBACK-START
------------------
-- DROP TABLE DIALOGMOTEBEHOV;

---------------
-- ROLLBACK-END

CREATE TABLE DIALOGMOTEBEHOV (
  dialogmotebehov_uuid        VARCHAR(36) NOT NULL,
  opprettet_dato              TIMESTAMP NOT NULL,
  opprettet_av                VARCHAR(13) NOT NULL,
  aktoer_id                   VARCHAR(13) NOT NULL,
  friskmelding_forventning    CLOB,
  tiltak                      CLOB,
  tiltak_resultat             CLOB,
  har_motebehov               NUMBER CHECK (har_motebehov IN (1, 0)),
  forklaring                  CLOB,
  CONSTRAINT DIALOGMOTEBEHOV_PK PRIMARY KEY (dialogmotebehov_uuid)
);

CREATE INDEX dialogmotebehov_uuid_index ON DIALOGMOTEBEHOV (dialogmotebehov_uuid);