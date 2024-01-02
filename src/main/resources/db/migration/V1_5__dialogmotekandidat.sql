CREATE TABLE DIALOGMOTEKANDIDAT
(
    id                               SERIAL PRIMARY KEY,
    uuid                             VARCHAR(36) NOT NULL UNIQUE,
    dialogmotekandidat_external_uuid VARCHAR(36) NOT NULL UNIQUE,
    person_ident                     VARCHAR(11) NOT NULL,
    kandidat                         VARCHAR(1)  NOT NULL,
    arsak                            VARCHAR(36) NOT NULL,
    created_at                       TIMESTAMP   NOT NULL,
    database_updated_at              TIMESTAMP   NOT NULL
);

CREATE INDEX dialogmotekandidat_personident_index ON DIALOGMOTEKANDIDAT(person_ident);
