-- https://confluence.adeo.no/display/fsys/Database

CREATE TABLE DIALOGMOTEBEHOV (
  dialogmotebehov_uuid       VARCHAR(100) NOT NULL PRIMARY KEY,
  tidspunkt_friskmelding     VARCHAR(1200),
  tiltak                     VARCHAR(1200),
  resultat_tiltak            VARCHAR(1200),
  trenger_mote               NUMBER CHECK(trenger_mote IN (1,0)),
  behov_dialogmote           VARCHAR(1200)
);