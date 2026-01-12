-- OPHJOD-2957: Update ESCO data staging tables and import procedures
DROP TABLE esco_data.occupations;
DROP TABLE esco_data.skills;

-- Staging tables for occupations (ammatti)
CREATE TABLE esco_data.occupations
(
  data jsonb,
  uri  TEXT PRIMARY KEY GENERATED ALWAYS AS (data ->> 'uri') STORED
);

CREATE TABLE esco_data.occupation_descriptions
(
  data jsonb,
  uri  TEXT PRIMARY KEY GENERATED ALWAYS AS (data ->> 'key') STORED
);

-- Staging tables for skills (osaaminen)
CREATE TABLE esco_data.skills
(
  data jsonb,
  uri  TEXT PRIMARY KEY GENERATED ALWAYS AS (data ->> 'uri') STORED
);

CREATE TABLE esco_data.skill_descriptions
(
  data jsonb,
  uri  TEXT PRIMARY KEY GENERATED ALWAYS AS (data ->> 'key') STORED
);

-- Import procedure for ammatti
CREATE OR REPLACE PROCEDURE esco_data.import_ammatti()
  LANGUAGE sql
BEGIN
  ATOMIC
  -- Insert/update ammatti records from occupations (skip null notation)
  INSERT INTO ammatti(uri, koodi)
  SELECT d.uri, d.data ->> 'notation' AS koodi
  FROM esco_data.occupations d
  WHERE d.data ->> 'notation' IS NOT NULL
  ON CONFLICT(uri) DO UPDATE SET koodi = excluded.koodi;

  -- Insert/update ammatti_kaannos with names from prefLabel
  INSERT INTO ammatti_kaannos(ammatti_id, kaannos_key, nimi)
  SELECT a.id, UPPER(j.key) AS kaannos_key, j.value AS nimi
  FROM ammatti a
         JOIN esco_data.occupations d ON a.uri = d.uri,
       JSONB_EACH_TEXT(d.data -> 'prefLabel') j
  WHERE UPPER(j.key) IN ('FI', 'SV', 'EN')
  ON CONFLICT(ammatti_id, kaannos_key) DO UPDATE SET nimi = excluded.nimi;

  -- Update kuvaus from occupation_descriptions
  WITH kuvaus AS (SELECT a.id, UPPER(j.key) AS kaannos_key, j.value AS kuvaus
                  FROM ammatti a
                         JOIN esco_data.occupation_descriptions d ON a.uri = d.uri,
                       JSONB_EACH_TEXT(d.data -> 'value') j
                  WHERE UPPER(j.key) IN ('FI', 'SV', 'EN'))
  UPDATE ammatti_kaannos ak
  SET kuvaus = k.kuvaus
  FROM kuvaus k
  WHERE ak.ammatti_id = k.id
    AND ak.kaannos_key = k.kaannos_key;

  INSERT INTO ammatti_versio AS v (versio)
  VALUES (1)
  ON CONFLICT (id) DO UPDATE SET versio = v.versio + 1;
END;

-- Import procedure for osaaminen
CREATE OR REPLACE PROCEDURE esco_data.import_osaaminen()
  LANGUAGE sql
BEGIN
  ATOMIC
  -- Insert/update osaaminen records from skills
  INSERT INTO osaaminen(uri)
  SELECT d.uri
  FROM esco_data.skills d
  ON CONFLICT(uri) DO NOTHING;

  -- Insert/update osaaminen_kaannos with names from prefLabel
  INSERT INTO osaaminen_kaannos(osaaminen_id, kaannos_key, nimi)
  SELECT o.id, UPPER(j.key) AS kaannos_key, j.value AS nimi
  FROM osaaminen o
         JOIN esco_data.skills d ON o.uri = d.uri,
       JSONB_EACH_TEXT(d.data -> 'prefLabel') j
  WHERE UPPER(j.key) IN ('FI', 'SV', 'EN')
  ON CONFLICT(osaaminen_id, kaannos_key) DO UPDATE SET nimi = excluded.nimi;

  -- Update kuvaus from skill_descriptions
  WITH kuvaus AS (SELECT o.id, UPPER(j.key) AS kaannos_key, j.value AS kuvaus
                  FROM osaaminen o
                         JOIN esco_data.skill_descriptions d ON o.uri = d.uri,
                       JSONB_EACH_TEXT(d.data -> 'value') j
                  WHERE UPPER(j.key) IN ('FI', 'SV', 'EN'))
  UPDATE osaaminen_kaannos ok
  SET kuvaus = k.kuvaus
  FROM kuvaus k
  WHERE ok.osaaminen_id = k.id
    AND ok.kaannos_key = k.kaannos_key;

  INSERT INTO osaaminen_versio AS v (versio)
  VALUES (1)
  ON CONFLICT (id) DO UPDATE SET versio = v.versio + 1;
END;
