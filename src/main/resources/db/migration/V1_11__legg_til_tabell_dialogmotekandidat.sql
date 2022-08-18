CREATE TABLE DIALOGMOTEKANDIDAT
(
    uuid                             VARCHAR(36) NOT NULL,
    dialogmotekandidat_external_uuid VARCHAR(36) NOT NULL,
    person_ident                     VARCHAR(11) NOT NULL,
    kandidat                         VARCHAR(1)  NOT NULL,
    arsak                            VARCHAR(36) NOT NULL,
    created_at                       TIMESTAMP   NOT NULL,
    database_updated_at              TIMESTAMP   NOT NULL,
    CONSTRAINT DIALOGMOTEKANDIDAT_PK PRIMARY KEY (uuid),
    CONSTRAINT DIALOGMOTEKANDIDAT_EXTERN_UUID_UNIQUE UNIQUE (dialogmotekandidat_external_uuid)
);
