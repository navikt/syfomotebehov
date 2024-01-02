CREATE TABLE OPPFOLGINGSTILFELLE (
  id                        SERIAL PRIMARY KEY,
  oppfolgingstilfelle_uuid  VARCHAR(36) NOT NULL UNIQUE,
  opprettet                 TIMESTAMP NOT NULL,
  sist_endret               TIMESTAMP NOT NULL,
  fnr                       VARCHAR(11) NOT NULL,
  virksomhetsnummer         VARCHAR(9)  NOT NULL,
  fom                       TIMESTAMP NOT NULL,
  tom                       TIMESTAMP NOT NULL,
  CONSTRAINT FNR_VIRKSOMHETSNR_UNIQUE UNIQUE (fnr, virksomhetsnummer)
);

CREATE INDEX oppfolgingstilfelle_fnr_index ON OPPFOLGINGSTILFELLE(fnr);
