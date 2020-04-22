-- ROLLBACK-START
------------------
-- DROP TABLE OPPFOLGINGSTILFELLE;

---------------
-- ROLLBACK-END

CREATE TABLE OPPFOLGINGSTILFELLE (
  oppfolgingstilfelle_uuid  VARCHAR(36) NOT NULL,
  opprettet                 TIMESTAMP NOT NULL,
  sist_endret               TIMESTAMP NOT NULL,
  aktoer_id                 VARCHAR(13) NOT NULL,
  virksomhetsnummer         VARCHAR(9)  NOT NULL,
  fom                       TIMESTAMP NOT NULL,
  tom                       TIMESTAMP NOT NULL,
  CONSTRAINT OPPFOLGINGSTILFELLE_PK PRIMARY KEY(oppfolgingstilfelle_uuid),
  CONSTRAINT AKTOR_VIRKSOMHETSNR_UNIQUE UNIQUE (aktoer_id, virksomhetsnummer)
);
