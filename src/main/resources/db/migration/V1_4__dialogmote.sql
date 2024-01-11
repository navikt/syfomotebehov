DROP TABLE IF EXISTS DIALOGMOTE;

CREATE TABLE DIALOGMOTE
(
    id                       SERIAL PRIMARY KEY,
    uuid                     VARCHAR(36) NOT NULL UNIQUE,
    dialogmote_extern_uuid   VARCHAR(36) NOT NULL,
    dialogmote_tidspunkt     TIMESTAMP   NOT NULL,
    status_endring_tidspunkt TIMESTAMP   NOT NULL,
    db_endring_tidspunkt     TIMESTAMP   NOT NULL,
    status_endring_type      VARCHAR(36) NOT NULL,
    person_ident             VARCHAR(11) NOT NULL,
    virksomhetsnummer        VARCHAR(9)  NOT NULL
);

CREATE INDEX dialogmote_personident_index ON DIALOGMOTE(person_ident);
