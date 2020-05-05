-- ROLLBACK-START
------------------
-- DROP TABLE OPPFOLGINGSTILFELLE;

---------------
-- ROLLBACK-END

DELETE FROM OPPFOLGINGSTILFELLE;

DROP TABLE OPPFOLGINGSTILFELLE;

CREATE TABLE OPPFOLGINGSTILFELLE (
  oppfolgingstilfelle_uuid  VARCHAR(36) NOT NULL,
  opprettet                 TIMESTAMP NOT NULL,
  sist_endret               TIMESTAMP NOT NULL,
  fnr                       VARCHAR(11) NOT NULL,
  virksomhetsnummer         VARCHAR(9)  NOT NULL,
  fom                       TIMESTAMP NOT NULL,
  tom                       TIMESTAMP NOT NULL,
  CONSTRAINT OPPFOLGINGSTILFELLE_PK PRIMARY KEY(oppfolgingstilfelle_uuid),
  CONSTRAINT FNR_VIRKSOMHETSNR_UNIQUE UNIQUE (fnr, virksomhetsnummer)
);
