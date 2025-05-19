--
-- Additional schema definitions
-- Eventually replaced by a migration tool
--
ALTER TABLE yksilo
  DROP CONSTRAINT IF EXISTS fk_yksilo_id,
  ADD CONSTRAINT fk_yksilo_id FOREIGN KEY (id) REFERENCES tunnistus.henkilo (yksilo_id)
;;;

CREATE UNIQUE INDEX IF NOT EXISTS ix_active_koulutusmahdollisuus ON koulutusmahdollisuus (id)
  WHERE aktiivinen = true
;;;

CREATE UNIQUE INDEX IF NOT EXISTS ix_active_tyomahdollisuus ON tyomahdollisuus (id)
    WHERE aktiivinen = true
;;;

--
-- KOULUTUSMAHDOLLISUUS data import
--
CREATE SCHEMA IF NOT EXISTS koulutusmahdollisuus_data
;;;

CREATE TABLE IF NOT EXISTS koulutusmahdollisuus_data.import (
  data jsonb,
  id   uuid generated always as ( (data ->> 'id')::uuid ) stored primary key
)
;;;

CREATE OR REPLACE PROCEDURE koulutusmahdollisuus_data.clear()
  LANGUAGE SQL AS
$$
  UPDATE koulutusmahdollisuus SET aktiivinen = false;
$$
;;;

CREATE OR REPLACE PROCEDURE koulutusmahdollisuus_data.import()
  LANGUAGE SQL
BEGIN
  ATOMIC

  -- Then koulutusmahdollisuus upsert, setting active to true for all records that will be imported
  INSERT INTO koulutusmahdollisuus(id, tyyppi, kesto_minimi, kesto_mediaani, kesto_maksimi, aktiivinen)
  SELECT id,
         data ->> 'tyyppi',
         (data ->> 'kestoMinimi')::float(53),
         (data ->> 'kestoMediaani')::float(53),
         (data ->> 'kestoMaksimi')::float(53),
         true -- Set aktiivinen to true for all imported records
  FROM koulutusmahdollisuus_data.import
  ON CONFLICT (id) DO UPDATE SET
                               tyyppi = EXCLUDED.tyyppi,
                               kesto_minimi = EXCLUDED.kesto_minimi,
                               kesto_mediaani = EXCLUDED.kesto_mediaani,
                               kesto_maksimi = EXCLUDED.kesto_maksimi,
                               aktiivinen = true; -- Ensure updated records are marked as active

  -- Upsert translations
  WITH translation_data AS (
    SELECT d.id,
           upper(x.key) as kaannos_key,
           x.value AS otsikko,
           y.value AS tiivistelma,
           z.value AS kuvaus
    FROM koulutusmahdollisuus_data.import d,
         jsonb_each_text(data -> 'otsikko') x
           LEFT JOIN LATERAL jsonb_each_text(data -> 'tiivistelma') y ON (x.key = y.key)
           LEFT JOIN LATERAL jsonb_each_text(data -> 'kuvaus') z ON x.key = z.key
  )
  INSERT INTO koulutusmahdollisuus_kaannos(koulutusmahdollisuus_id, kaannos_key, otsikko, tiivistelma, kuvaus)
  SELECT id, kaannos_key, otsikko, tiivistelma, kuvaus
  FROM translation_data
  ON CONFLICT (koulutusmahdollisuus_id, kaannos_key) DO UPDATE SET
                                                                 otsikko = EXCLUDED.otsikko,
                                                                 tiivistelma = EXCLUDED.tiivistelma,
                                                                 kuvaus = EXCLUDED.kuvaus;

  -- Delete related koulutus_viite_kaannos records first
  DELETE FROM koulutus_viite_kaannos
  WHERE koulutus_viite_id IN (
    SELECT kv.id
    FROM koulutus_viite kv
    WHERE kv.koulutusmahdollisuus_id IN (SELECT id FROM koulutusmahdollisuus_data.import)
  );

  -- Delete parent koulutus_viite records
  DELETE FROM koulutus_viite
  WHERE koulutusmahdollisuus_id IN (SELECT id FROM koulutusmahdollisuus_data.import);

  -- Insert new references
  WITH koulutukset AS (
    SELECT DISTINCT d.id, k.oid, k.nimi
    FROM koulutusmahdollisuus_data.import d,
         jsonb_to_recordset(data -> 'koulutukset') AS k(oid varchar, nimi jsonb)
  ),
       viitteet AS (
         INSERT INTO koulutus_viite (oid, koulutusmahdollisuus_id)
           SELECT oid, id
           FROM koulutukset
           RETURNING id, oid, koulutusmahdollisuus_id
       ),
       translation_keys AS (
         SELECT v.id as koulutus_viite_id,
                upper(x.key) as kaannos_key,
                x.value as nimi
         FROM viitteet v
                JOIN koulutukset k ON (v.koulutusmahdollisuus_id = k.id AND v.oid = k.oid),
              jsonb_each_text(k.nimi) x
       )
  INSERT INTO koulutus_viite_kaannos(koulutus_viite_id, kaannos_key, nimi)
  SELECT koulutus_viite_id, kaannos_key, nimi
  FROM translation_keys;

  -- Delete koulutusmahdollisuus_jakauma_arvot first
  DELETE FROM koulutusmahdollisuus_jakauma_arvot
  WHERE koulutusmahdollisuus_jakauma_id IN (
    SELECT kmj.id
    FROM koulutusmahdollisuus_jakauma kmj
    WHERE kmj.koulutusmahdollisuus_id IN (SELECT id FROM koulutusmahdollisuus_data.import)
  );

  -- Delete parent koulutusmahdollisuus_jakauma records
  DELETE FROM koulutusmahdollisuus_jakauma
  WHERE koulutusmahdollisuus_id IN (SELECT id FROM koulutusmahdollisuus_data.import);

  -- Insert new jakauma records
  WITH paths AS (
    SELECT n, p::jsonpath
    FROM (values ('OSAAMINEN', '$.osaamiset'),
                 ('KOULUTUSALA', '$.koulutusalaJakauma'),
                 ('MAKSULLISUUS', '$.maksullisuusJakauma'),
                 ('OPETUSTAPA', '$.opetustapaJakauma'),
                 ('AIKA', '$.aikaJakauma'),
                 ('KUNTA', '$.kuntaJakauma')) AS x(n, p)
  ),
       jakaumat AS (
         INSERT INTO koulutusmahdollisuus_jakauma (koulutusmahdollisuus_id, tyyppi, maara, tyhjia)
           SELECT d.id,
                  p.n,
                  (jsonb_path_query_first(d.data, p.p) -> 'kokonaismaara')::int,
                  (jsonb_path_query_first(d.data, p.p) -> 'tyhjienMaara')::int
           FROM koulutusmahdollisuus_data.import d,
                paths p
           WHERE jsonb_path_exists(d.data, p.p)
           RETURNING id, koulutusmahdollisuus_id, tyyppi
       ),
       distribution_values AS (
         SELECT j.id as koulutusmahdollisuus_jakauma_id,
                x.arvo as arvo,
                x.prosenttiOsuus as osuus
         FROM paths p
                JOIN jakaumat j ON (p.n = j.tyyppi)
                JOIN koulutusmahdollisuus_data.import d ON (j.koulutusmahdollisuus_id = d.id),
              jsonb_to_recordset(jsonb_path_query_first(d.data, p.p) -> 'arvot') as x(arvo text, prosenttiOsuus float(53))
       )
  INSERT INTO koulutusmahdollisuus_jakauma_arvot(koulutusmahdollisuus_jakauma_id, arvo, osuus)
  SELECT koulutusmahdollisuus_jakauma_id, arvo, osuus
  FROM distribution_values;
END
;;;

--
-- TYÖMAHDOLLISUUS data import
--
CREATE SCHEMA IF NOT EXISTS tyomahdollisuus_data
;;;

CREATE TABLE IF NOT EXISTS tyomahdollisuus_data.import (
  data jsonb not null,
  id   uuid generated always as ( (data ->> 'id')::uuid ) stored primary key
)
;;;

CREATE OR REPLACE PROCEDURE tyomahdollisuus_data.clear()
  LANGUAGE SQL AS
$$
  UPDATE tyomahdollisuus  SET aktiivinen = false;
$$
;;;

CREATE OR REPLACE PROCEDURE tyomahdollisuus_data.import()
  LANGUAGE sql
BEGIN
  ATOMIC

  -- Then perform tyomahdollisuus upsert, setting active to true for all imported records
  INSERT INTO tyomahdollisuus(id, ammattiryhma, aineisto, aktiivinen)
  SELECT i.id,
         i.data ->> 'ammattiryhma',
         i.data ->> 'aineisto',
         true -- Set aktiivinen to true for all imported records
  FROM tyomahdollisuus_data.import i
  ON CONFLICT (id) DO UPDATE SET
                               ammattiryhma = EXCLUDED.ammattiryhma,
                               aineisto = EXCLUDED.aineisto,
                               aktiivinen = true; -- Ensure updated records are marked as active

  -- Delete existing translations for imported records
  DELETE FROM tyomahdollisuus_kaannos
  WHERE tyomahdollisuus_id IN (SELECT id FROM tyomahdollisuus_data.import);

  -- Insert translations
  INSERT INTO tyomahdollisuus_kaannos(tyomahdollisuus_id, kaannos_key, otsikko,
                                      tiivistelma,
                                      kuvaus,
                                      tehtavat,
                                      yleiset_vaatimukset)
  SELECT d.id,
         CASE WHEN o.key = 'se' THEN 'SV' ELSE upper(o.key) END,
         o.value AS nimi,
         t.value AS tiivistelma,
         k.value AS kuvaus,
         (SELECT string_agg(elem, E'\n')
          FROM jsonb_array_elements_text(tt.value::jsonb) elem) AS tehtavat,
         yv.value AS yleiset_vaatimukset
  FROM tyomahdollisuus_data.import d,
       jsonb_each_text(d.data -> 'perustiedot' -> 'tyomahdollisuudenOtsikko') o
         LEFT JOIN LATERAL jsonb_each_text(d.data -> 'perustiedot' -> 'tyomahdollisuudenTiivistelma') t
                   ON (o.key = t.key)
         LEFT JOIN LATERAL jsonb_each_text(d.data -> 'perustiedot' -> 'tyomahdollisuudenKuvaus') k
                   ON o.key = k.key
         LEFT JOIN LATERAL jsonb_each_text(jsonb_path_query_first(d.data, '$.perustiedot.tyomahdollisuudenTehtavat ? (@ != null)')) tt
                   ON o.key = tt.key
         LEFT JOIN LATERAL jsonb_each_text(jsonb_path_query_first(d.data, '$.perustiedot.tyomahdollisuudenYleisetVaatimukset ? (@ != null)')) yv
                   ON o.key = yv.key;

  -- Delete existing jakaumat for imported records
  DELETE FROM tyomahdollisuus_jakauma_arvot
  WHERE tyomahdollisuus_jakauma_id IN (
    SELECT id
    FROM tyomahdollisuus_jakauma
    WHERE tyomahdollisuus_id IN (SELECT id FROM tyomahdollisuus_data.import)
  );

  DELETE FROM tyomahdollisuus_jakauma
  WHERE tyomahdollisuus_id IN (SELECT id FROM tyomahdollisuus_data.import);

  -- Insert jakauma data
  WITH paths AS (SELECT n, p::jsonpath
                 FROM (values ('OSAAMINEN', '$.osaamisvaatimukset.osaamiset'),
                              ('AMMATTI', '$.osaamisvaatimukset.ammatit'),
                              ('AJOKORTTI', '$.osaamisvaatimukset.ajokorttiJakauma'),
                              ('KIELITAITO', '$.osaamisvaatimukset.kielitaitoJakauma'),
                              ('KORTIT_JA_LUVAT', '$.osaamisvaatimukset.kortitJaLuvatJakauma'),
                              ('KOULUTUSASTE', '$.osaamisvaatimukset.koulutusasteJakauma'),
                              ('RIKOSREKISTERIOTE',
                               '$.osaamisvaatimukset.rikosrekisterioteJakauma'),
                              ('MATKUSTUSVAATIMUS', '$.perustiedot.matkustamisvaatimusJakauma'),
                              ('PALKAN_PERUSTE', '$.perustiedot.palkanPerusteJakauma'),
                              ('PALVELUSSUHDE', '$.perustiedot.palvelussuhdeJakauma'),
                              ('TYOAIKA', '$.perustiedot.tyoaikaJakauma'),
                              ('TYON_JATKUVUUS', '$.perustiedot.tyonJatkuvuusJakauma'),
                              ('KUNTA', '$.sijainti.kuntaJakauma'),
                              ('MAAKUNTA', '$.sijainti.maakuntaJakauma'),
                              ('MAA', '$.sijainti.maaJakauma'),
                              ('SIJAINTI_JOUSTAVA', '$.sijainti.sijaintiJoustavaJakauma'),
                              ('TYOKIELI', '$.tyokieletJakauma')) AS x(n, p)),
       jakaumat AS (
         INSERT INTO tyomahdollisuus_jakauma (tyomahdollisuus_id, tyyppi, maara, tyhjia)
           SELECT d.id,
                  p.n,
                  (jsonb_path_query_first(d.data, p.p) -> 'kokonaismaara')::int,
                  (jsonb_path_query_first(d.data, p.p) -> 'tyhjienMaara')::int
           FROM tyomahdollisuus_data.import d,
                paths p
           WHERE jsonb_path_exists(d.data, p.p)
           RETURNING id, tyomahdollisuus_id, tyyppi
       ),
       distribution_values AS (
         SELECT j.id as tyomahdollisuus_jakauma_id,
                x.arvo as arvo,
                x.prosenttiOsuus as osuus
         FROM paths p
                JOIN jakaumat j ON (p.n = j.tyyppi)
                JOIN tyomahdollisuus_data.import d ON (j.tyomahdollisuus_id = d.id),
              jsonb_to_recordset(jsonb_path_query_first(d.data, p.p) -> 'arvot') as x(arvo text, prosenttiOsuus float(53))
       )
  INSERT INTO tyomahdollisuus_jakauma_arvot(tyomahdollisuus_jakauma_id, arvo, osuus)
  SELECT tyomahdollisuus_jakauma_id, arvo, osuus
  FROM distribution_values;
END
;;;


--
-- ESCO DATA import
--

CREATE SCHEMA IF NOT EXISTS esco_data;

CREATE TABLE IF NOT EXISTS esco_data.skills (
  conceptUri text not null,
  lang text not null default current_setting('esco.lang'),
  preferredLabel text not null,
  description text,
  conceptType text,
  skillType text,
  reuseLevel text,
  altLabels text,
  hiddenLabels text,
  status text,
  modifiedDate text,
  scopeNote text,
  definition text,
  inScheme text,
  primary key (conceptUri, lang)
  );

CREATE TABLE IF NOT EXISTS esco_data.occupations (
  conceptType             text not null,
  conceptUri              text not null,
  lang                    text not null default current_setting('esco.lang'),
  iscoGroup               text,
  preferredLabel          text not null,
  altLabels               text,
  hiddenLabels            text,
  status                  text,
  modifiedDate            text,
  regulatedProfessionNote text,
  scopeNote               text,
  definition              text,
  inScheme                text,
  description             text,
  code                    text not null,
  primary key (conceptUri, lang)
);

CREATE OR REPLACE PROCEDURE esco_data.import_osaaminen()
  LANGUAGE SQL AS
$$
INSERT INTO osaaminen(uri)
SELECT DISTINCT conceptUri
FROM esco_data.skills
  ON CONFLICT(uri) DO NOTHING;

INSERT INTO osaaminen_kaannos (osaaminen_id, kaannos_key, kuvaus, nimi)
SELECT o.id, upper(s.lang), s.description, s.preferredLabel
FROM osaaminen o
       JOIN esco_data.skills s on (o.uri = s.conceptUri)
  ON CONFLICT(osaaminen_id, kaannos_key)
  DO UPDATE SET kuvaus = excluded.kuvaus,
         nimi   = excluded.nimi;

INSERT INTO osaaminen_versio AS v (versio)
VALUES (1)
ON CONFLICT (id) DO UPDATE SET versio = v.versio + 1;
$$
;;;

CREATE OR REPLACE PROCEDURE esco_data.import_ammatti()
  LANGUAGE SQL AS
$$
INSERT INTO ammatti(uri, koodi)
SELECT DISTINCT conceptUri, code
FROM esco_data.occupations
ON CONFLICT(uri) DO NOTHING;

INSERT INTO ammatti_kaannos (ammatti_id, kaannos_key, kuvaus, nimi)
SELECT a.id, upper(o.lang), o.description, o.preferredLabel
FROM ammatti a
       JOIN esco_data.occupations o on (a.uri = o.conceptUri)
ON CONFLICT(ammatti_id, kaannos_key)
  DO UPDATE SET kuvaus = excluded.kuvaus,
                nimi   = excluded.nimi;

INSERT INTO ammatti_versio AS v (versio)
VALUES (1)
ON CONFLICT (id) DO UPDATE SET versio = v.versio + 1;
$$
;;;
