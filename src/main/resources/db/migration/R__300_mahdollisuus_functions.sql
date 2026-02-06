-- ========================
-- Työmahdollisuus import
-- ========================
CREATE TABLE IF NOT EXISTS tyomahdollisuus_data.import
(
  data jsonb NOT NULL,
  id   uuid GENERATED ALWAYS AS ( (data ->> 'id')::uuid ) STORED PRIMARY KEY
);

CREATE OR REPLACE PROCEDURE tyomahdollisuus_data.clear()
  LANGUAGE SQL
BEGIN
  ATOMIC
  UPDATE tyomahdollisuus
  SET aktiivinen = FALSE;
END;

CREATE OR REPLACE PROCEDURE tyomahdollisuus_data.import()
  LANGUAGE sql
BEGIN
  ATOMIC

  INSERT INTO tyomahdollisuus(id, ammattiryhma, aineisto, aktiivinen)
  SELECT i.id,
         i.data ->> 'ammattiryhma',
         i.data ->> 'aineisto',
         TRUE
  FROM tyomahdollisuus_data.import i
  ON CONFLICT (id) DO UPDATE SET ammattiryhma = EXCLUDED.ammattiryhma,
                                 aineisto     = EXCLUDED.aineisto,
                                 aktiivinen   = TRUE;
  DELETE
  FROM tyomahdollisuus_kaannos
  WHERE tyomahdollisuus_id IN (SELECT id FROM tyomahdollisuus_data.import);

  INSERT INTO tyomahdollisuus_kaannos(tyomahdollisuus_id, kaannos_key, otsikko,
                                      tiivistelma,
                                      kuvaus,
                                      tehtavat,
                                      yleiset_vaatimukset)
  SELECT d.id,
         CASE WHEN o.key = 'se' THEN 'SV' ELSE UPPER(o.key) END,
         o.value AS nimi,
         t.value AS tiivistelma,
         k.value AS kuvaus,
         (SELECT STRING_AGG(elem, E'\n')
          FROM JSONB_ARRAY_ELEMENTS_TEXT(tt.value::jsonb) elem) AS tehtavat,
         yv.value AS yleiset_vaatimukset
  FROM tyomahdollisuus_data.import d,
       JSONB_EACH_TEXT(d.data -> 'perustiedot' -> 'tyomahdollisuudenOtsikko') o
         LEFT JOIN LATERAL JSONB_EACH_TEXT(d.data -> 'perustiedot' -> 'tyomahdollisuudenTiivistelma') t
                   ON (o.key = t.key)
         LEFT JOIN LATERAL JSONB_EACH_TEXT(d.data -> 'perustiedot' -> 'tyomahdollisuudenKuvaus') k
                   ON o.key = k.key
         LEFT JOIN LATERAL JSONB_EACH_TEXT(JSONB_PATH_QUERY_FIRST(d.data,
                                                                  '$.perustiedot.tyomahdollisuudenTehtavat ? (@ != null)')) tt
                   ON o.key = tt.key
         LEFT JOIN LATERAL JSONB_EACH_TEXT(JSONB_PATH_QUERY_FIRST(d.data,
                                                                  '$.perustiedot.tyomahdollisuudenYleisetVaatimukset ? (@ != null)')) yv
                   ON o.key = yv.key;

  DELETE
  FROM tyomahdollisuus_jakauma_arvot
  WHERE tyomahdollisuus_jakauma_id IN (SELECT id
                                       FROM tyomahdollisuus_jakauma
                                       WHERE tyomahdollisuus_id IN
                                             (SELECT id FROM tyomahdollisuus_data.import));

  DELETE
  FROM tyomahdollisuus_jakauma
  WHERE tyomahdollisuus_id IN (SELECT id FROM tyomahdollisuus_data.import);

  WITH paths AS (SELECT n, p::jsonpath
                 FROM (VALUES ('OSAAMINEN', '$.osaamisvaatimukset.osaamiset'),
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
                              ('TYOKIELI', '$.tyokieletJakauma'),
                              ('TOIMIALA', '$.perustiedot.toimialaJakauma'))
                        AS x(n, p)),
       jakaumat AS (
         INSERT INTO tyomahdollisuus_jakauma (tyomahdollisuus_id, tyyppi, maara, tyhjia)
           SELECT d.id,
                  p.n,
                  (JSONB_PATH_QUERY_FIRST(d.data, p.p) -> 'kokonaismaara')::INT,
                  (JSONB_PATH_QUERY_FIRST(d.data, p.p) -> 'tyhjienMaara')::INT
           FROM tyomahdollisuus_data.import d,
                paths p
           WHERE JSONB_PATH_EXISTS(d.data, p.p)
           RETURNING id, tyomahdollisuus_id, tyyppi),
       distribution_values AS (SELECT j.id AS tyomahdollisuus_jakauma_id,
                                      x.arvo AS arvo,
                                      x.prosenttiosuus AS osuus
                               FROM paths p
                                      JOIN jakaumat j ON (p.n = j.tyyppi)
                                      JOIN tyomahdollisuus_data.import d
                                           ON (j.tyomahdollisuus_id = d.id),
                                    JSONB_TO_RECORDSET(JSONB_PATH_QUERY_FIRST(d.data, p.p) -> 'arvot') AS x(arvo TEXT, prosenttiosuus FLOAT(53)))
  INSERT
  INTO tyomahdollisuus_jakauma_arvot(tyomahdollisuus_jakauma_id, arvo, osuus)
  SELECT tyomahdollisuus_jakauma_id, arvo, osuus
  FROM distribution_values;
END;

-- ===========================
-- Koulutusmahdollisuus import
-- ===========================
CREATE TABLE IF NOT EXISTS koulutusmahdollisuus_data.import
(
  data jsonb,
  id   uuid GENERATED ALWAYS AS ( (data ->> 'id')::uuid ) STORED PRIMARY KEY
);

CREATE OR REPLACE PROCEDURE koulutusmahdollisuus_data.clear()
  LANGUAGE SQL
BEGIN
  ATOMIC
  UPDATE koulutusmahdollisuus
  SET aktiivinen = FALSE;
END;


CREATE OR REPLACE PROCEDURE koulutusmahdollisuus_data.import()
  LANGUAGE SQL
BEGIN
  ATOMIC

  -- Then koulutusmahdollisuus upsert, setting active to true for all records that will be imported
  INSERT INTO koulutusmahdollisuus(id, tyyppi, kesto_minimi, kesto_mediaani, kesto_maksimi,
                                   aktiivinen)
  SELECT id,
         data ->> 'tyyppi',
         (data ->> 'kestoMinimi')::FLOAT(53),
         (data ->> 'kestoMediaani')::FLOAT(53),
         (data ->> 'kestoMaksimi')::FLOAT(53),
         TRUE -- Set aktiivinen to true for all imported records
  FROM koulutusmahdollisuus_data.import
  ON CONFLICT (id) DO UPDATE SET tyyppi         = EXCLUDED.tyyppi,
                                 kesto_minimi   = EXCLUDED.kesto_minimi,
                                 kesto_mediaani = EXCLUDED.kesto_mediaani,
                                 kesto_maksimi  = EXCLUDED.kesto_maksimi,
                                 aktiivinen     = TRUE;

  -- Upsert translations
  WITH translation_data AS (SELECT d.id,
                                   UPPER(x.key) AS kaannos_key,
                                   x.value AS otsikko,
                                   y.value AS tiivistelma,
                                   z.value AS kuvaus
                            FROM koulutusmahdollisuus_data.import d,
                                 JSONB_EACH_TEXT(data -> 'otsikko') x
                                   LEFT JOIN LATERAL JSONB_EACH_TEXT(data -> 'tiivistelma') y
                                             ON (x.key = y.key)
                                   LEFT JOIN LATERAL JSONB_EACH_TEXT(data -> 'kuvaus') z
                                             ON x.key = z.key)
  INSERT
  INTO koulutusmahdollisuus_kaannos(koulutusmahdollisuus_id, kaannos_key, otsikko, tiivistelma,
                                    kuvaus)
  SELECT id, kaannos_key, otsikko, tiivistelma, kuvaus
  FROM translation_data
  ON CONFLICT (koulutusmahdollisuus_id, kaannos_key) DO UPDATE SET otsikko     = EXCLUDED.otsikko,
                                                                   tiivistelma = EXCLUDED.tiivistelma,
                                                                   kuvaus      = EXCLUDED.kuvaus;

  -- Delete related koulutus_viite_kaannos records first
  DELETE
  FROM koulutus_viite_kaannos
  WHERE koulutus_viite_id IN (SELECT kv.id
                              FROM koulutus_viite kv
                              WHERE kv.koulutusmahdollisuus_id IN
                                    (SELECT id FROM koulutusmahdollisuus_data.import));

  -- Delete parent koulutus_viite records
  DELETE
  FROM koulutus_viite
  WHERE koulutusmahdollisuus_id IN (SELECT id FROM koulutusmahdollisuus_data.import);

  -- Insert new references
  WITH koulutukset AS (SELECT DISTINCT d.id, k.oid, k.nimi
                       FROM koulutusmahdollisuus_data.import d,
                            JSONB_TO_RECORDSET(data -> 'koulutukset') AS k(oid VARCHAR, nimi jsonb)),
       viitteet AS (
         INSERT INTO koulutus_viite (oid, koulutusmahdollisuus_id)
           SELECT oid, id
           FROM koulutukset
           RETURNING id, oid, koulutusmahdollisuus_id),
       translation_keys AS (SELECT v.id AS koulutus_viite_id,
                                   UPPER(x.key) AS kaannos_key,
                                   x.value AS nimi
                            FROM viitteet v
                                   JOIN koulutukset k
                                        ON (v.koulutusmahdollisuus_id = k.id AND v.oid = k.oid),
                                 JSONB_EACH_TEXT(k.nimi) x)
  INSERT
  INTO koulutus_viite_kaannos(koulutus_viite_id, kaannos_key, nimi)
  SELECT koulutus_viite_id, kaannos_key, nimi
  FROM translation_keys;

  -- Delete koulutusmahdollisuus_jakauma_arvot first
  DELETE
  FROM koulutusmahdollisuus_jakauma_arvot
  WHERE koulutusmahdollisuus_jakauma_id IN (SELECT kmj.id
                                            FROM koulutusmahdollisuus_jakauma kmj
                                            WHERE kmj.koulutusmahdollisuus_id IN
                                                  (SELECT id FROM koulutusmahdollisuus_data.import));

  -- Delete parent koulutusmahdollisuus_jakauma records
  DELETE
  FROM koulutusmahdollisuus_jakauma
  WHERE koulutusmahdollisuus_id IN (SELECT id FROM koulutusmahdollisuus_data.import);

  -- Insert new jakauma records
  WITH paths AS (SELECT n, p::jsonpath
                 FROM (VALUES ('OSAAMINEN', '$.osaamiset'),
                              ('KOULUTUSALA', '$.koulutusalaJakauma'),
                              ('MAKSULLISUUS', '$.maksullisuusJakauma'),
                              ('OPETUSTAPA', '$.opetustapaJakauma'),
                              ('AIKA', '$.aikaJakauma'),
                              ('KUNTA', '$.sijainti.kuntaJakauma'),
                              ('MAAKUNTA', '$.sijainti.maakuntaJakauma')) AS x(n, p)),
       jakaumat AS (
         INSERT INTO koulutusmahdollisuus_jakauma (koulutusmahdollisuus_id, tyyppi, maara, tyhjia)
           SELECT d.id,
                  p.n,
                  (JSONB_PATH_QUERY_FIRST(d.data, p.p) -> 'kokonaismaara')::INT,
                  (JSONB_PATH_QUERY_FIRST(d.data, p.p) -> 'tyhjienMaara')::INT
           FROM koulutusmahdollisuus_data.import d,
                paths p
           WHERE JSONB_PATH_EXISTS(d.data, p.p)
           RETURNING id, koulutusmahdollisuus_id, tyyppi),
       distribution_values AS (SELECT j.id AS koulutusmahdollisuus_jakauma_id,
                                      x.arvo AS arvo,
                                      x.prosenttiosuus AS osuus
                               FROM paths p
                                      JOIN jakaumat j ON (p.n = j.tyyppi)
                                      JOIN koulutusmahdollisuus_data.import d
                                           ON (j.koulutusmahdollisuus_id = d.id),
                                    JSONB_TO_RECORDSET(JSONB_PATH_QUERY_FIRST(d.data, p.p) -> 'arvot') AS x(arvo TEXT, prosenttiosuus FLOAT(53)))
  INSERT
  INTO koulutusmahdollisuus_jakauma_arvot(koulutusmahdollisuus_jakauma_id, arvo, osuus)
  SELECT koulutusmahdollisuus_jakauma_id, arvo, osuus
  FROM distribution_values;
END;
