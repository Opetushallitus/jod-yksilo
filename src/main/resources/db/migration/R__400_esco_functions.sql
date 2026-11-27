-- ========================
-- ESCO import
--
-- Staging tables hold one ESCO concept per row: the concept JSON with its
-- description merged in as a "description" language map (see scripts/esco-to-jsonl.sh).
-- ========================
CREATE TABLE IF NOT EXISTS esco_data.occupations
(
  data jsonb,
  uri  TEXT PRIMARY KEY GENERATED ALWAYS AS (data ->> 'uri') STORED
);

CREATE TABLE IF NOT EXISTS esco_data.skills
(
  data jsonb,
  uri  TEXT PRIMARY KEY GENERATED ALWAYS AS (data ->> 'uri') STORED
);

CREATE OR REPLACE PROCEDURE esco_data.import_ammatti()
  LANGUAGE sql
  SECURITY DEFINER
BEGIN
  ATOMIC
  -- Insert/update ammatti records from occupations (skip null notation)
  INSERT INTO ammatti(uri, koodi)
  SELECT d.uri, d.data ->> 'notation' AS koodi
  FROM esco_data.occupations d
  WHERE d.data ->> 'notation' IS NOT NULL
  ON CONFLICT(uri) DO UPDATE SET koodi = excluded.koodi;

  INSERT INTO ammatti_kaannos(ammatti_id, kaannos_key, nimi, kuvaus)
  SELECT a.id, UPPER(n.key) AS kaannos_key, n.value AS nimi, d.data -> 'description' ->> n.key AS kuvaus
  FROM ammatti a
         JOIN esco_data.occupations d ON a.uri = d.uri,
       JSONB_EACH_TEXT(d.data -> 'prefLabel') n
  WHERE UPPER(n.key) IN ('FI', 'SV', 'EN')
  ON CONFLICT(ammatti_id, kaannos_key) DO UPDATE SET nimi   = excluded.nimi,
                                                     kuvaus = excluded.kuvaus;

  INSERT INTO ammatti_versio AS v (versio)
  VALUES (1)
  ON CONFLICT (id) DO UPDATE SET versio = v.versio + 1;
END;

CREATE OR REPLACE PROCEDURE esco_data.import_osaaminen()
  LANGUAGE sql
  SECURITY DEFINER
BEGIN
  ATOMIC
  INSERT INTO osaaminen(uri)
  SELECT d.uri
  FROM esco_data.skills d
  ON CONFLICT(uri) DO NOTHING;

  INSERT INTO osaaminen_kaannos(osaaminen_id, kaannos_key, nimi, kuvaus)
  SELECT o.id, UPPER(n.key) AS kaannos_key, n.value AS nimi, d.data -> 'description' ->> n.key AS kuvaus
  FROM osaaminen o
         JOIN esco_data.skills d ON o.uri = d.uri,
       JSONB_EACH_TEXT(d.data -> 'prefLabel') n
  WHERE UPPER(n.key) IN ('FI', 'SV', 'EN')
  ON CONFLICT(osaaminen_id, kaannos_key) DO UPDATE SET nimi   = excluded.nimi,
                                                       kuvaus = excluded.kuvaus;

  INSERT INTO osaaminen_versio AS v (versio)
  VALUES (1)
  ON CONFLICT (id) DO UPDATE SET versio = v.versio + 1;
END;

-- The procedures run as their owner, so the data loader can import data without write access to
-- the target tables. SQL-standard bodies are bound at creation time, so search_path is not a concern.
REVOKE ALL ON PROCEDURE esco_data.import_ammatti(), esco_data.import_osaaminen() FROM PUBLIC;
GRANT EXECUTE ON PROCEDURE esco_data.import_ammatti(), esco_data.import_osaaminen() TO dataloader;
