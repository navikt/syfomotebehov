CREATE TABLE DIALOGMOTER
(
    uuid                     VARCHAR(36) NOT NULL,
    dialogmote_uuid          VARCHAR(36) NOT NULL,
    dialogmote_tidspunkt     TIMESTAMP   NOT NULL,
    status_endring_tidspunkt TIMESTAMP   NOT NULL,
    db_endring_tidspunkt       TIMESTAMP NOT NULL,
    status_endring_type      VARCHAR(36) NOT NULL,
    person_ident             VARCHAR(11) NOT NULL,
    virksomhetsnummer        VARCHAR(9)  NOT NULL,
    CONSTRAINT DIALOGMOTER_PK PRIMARY KEY (uuid),
    CONSTRAINT DIALOGMOTE_UUID_UNIQUE UNIQUE (dialogmote_uuid)
);
