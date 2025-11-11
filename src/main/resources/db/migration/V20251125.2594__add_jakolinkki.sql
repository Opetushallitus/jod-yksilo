CREATE TABLE yksilo.jakolinkki (
  id                                    uuid PRIMARY KEY,
  nimi                                  text,
  muistiinpano                          text,
  kotikunta_jaettu                      boolean NOT NULL DEFAULT false,
  syntymavuosi_jaettu                   boolean NOT NULL DEFAULT false,
  muu_osaaminen_jaettu                  boolean NOT NULL DEFAULT false,
  kiinnostukset_jaettu                  boolean NOT NULL DEFAULT false,
  tyomahdollisuus_suosikit_jaettu       boolean NOT NULL DEFAULT false,
  koulutusmahdollisuus_suosikit_jaettu  boolean NOT NULL DEFAULT false,
  luotu                                 timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  muokattu                              timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  yksilo_id                             uuid NOT NULL,
  CONSTRAINT fk_jakolinkki_yksilo FOREIGN KEY (yksilo_id) REFERENCES yksilo.yksilo (id) ON DELETE CASCADE
);

CREATE TABLE yksilo.jakolinkki_tyopaikat (
  jakolinkki_id       uuid NOT NULL,
  tyopaikka_id        uuid NOT NULL,
  CONSTRAINT pk_jakolinkki_tyopaikka PRIMARY KEY (jakolinkki_id, tyopaikka_id),
  CONSTRAINT fk_jakolinkki_tyopaikka_jakolinkki FOREIGN KEY (jakolinkki_id) REFERENCES yksilo.jakolinkki (id) ON DELETE CASCADE,
  CONSTRAINT fk_jakolinkki_tyopaikka_tyopaikka FOREIGN KEY (tyopaikka_id) REFERENCES yksilo.tyopaikka (id) ON DELETE CASCADE
);

CREATE INDEX idx_jakolinkki_tyopaikka_jakolinkki ON yksilo.jakolinkki_tyopaikat USING btree (jakolinkki_id);

CREATE TABLE yksilo.jakolinkki_koulutukset (
  jakolinkki_id             uuid NOT NULL,
  koulutus_kokonaisuus_id   uuid NOT NULL,
  CONSTRAINT pk_jakolinkki_koulutus PRIMARY KEY (jakolinkki_id, koulutus_kokonaisuus_id),
  CONSTRAINT fk_jakolinkki_koulutus_jakolinkki FOREIGN KEY (jakolinkki_id) REFERENCES yksilo.jakolinkki (id) ON DELETE CASCADE,
  CONSTRAINT fk_jakolinkki_koulutus_koulutus FOREIGN KEY (koulutus_kokonaisuus_id) REFERENCES yksilo.koulutus_kokonaisuus (id) ON DELETE CASCADE
);

CREATE INDEX idx_jakolinkki_koulutus_jakolinkki ON yksilo.jakolinkki_koulutukset USING btree (jakolinkki_id);

CREATE TABLE yksilo.jakolinkki_toiminnot (
  jakolinkki_id       uuid NOT NULL,
  toiminto_id         uuid NOT NULL,
  CONSTRAINT pk_jakolinkki_toiminto PRIMARY KEY (jakolinkki_id, toiminto_id),
  CONSTRAINT fk_jakolinkki_toiminto_jakolinkki FOREIGN KEY (jakolinkki_id) REFERENCES yksilo.jakolinkki (id) ON DELETE CASCADE,
  CONSTRAINT fk_jakolinkki_toiminto_toiminto FOREIGN KEY (toiminto_id) REFERENCES yksilo.toiminto (id) ON DELETE CASCADE
);

CREATE INDEX idx_jakolinkki_toiminto_jakolinkki ON yksilo.jakolinkki_toiminnot USING btree (jakolinkki_id);

