CREATE SCHEMA IF NOT EXISTS tunnistus;

CREATE TABLE tunnistus.henkilo (
  yksilo_id    UUID PRIMARY KEY,
  henkilo_id   VARCHAR(300),
  oppijanumero VARCHAR(300) UNIQUE,
  etunimi      TEXT,
  sukunimi     TEXT,
  email        VARCHAR(254),
  muokattu     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_henkilo_identifier CHECK (henkilo_id IS NOT NULL OR oppijanumero IS NOT NULL)
);

