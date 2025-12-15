CREATE TEMPORARY TABLE koulutuskoodi_import (
  koodi jsonb
);
COPY koulutuskoodi_import (koodi) FROM STDIN (FORMAT text);

-- expects that the json data per koodi is obtained from OPH koodisto-service
INSERT INTO koulutuskoodi(koodi)
  SELECT koodi->>'koodiArvo' FROM koulutuskoodi_import
ON CONFLICT (koodi) DO NOTHING;
INSERT INTO koulutuskoodi_kaannos(koulutuskoodi_id, kaannos_key, nimi, kuvaus)
  SELECT k.id, m.kieli, m.nimi, m.kuvaus FROM
  koulutuskoodi k,
  koulutuskoodi_import d,
  jsonb_to_recordset(d.koodi->'metadata') as m(nimi text, kieli text, kuvaus text)
  WHERE k.koodi = d.koodi->>'koodiArvo'
ON CONFLICT (koulutuskoodi_id, kaannos_key) DO UPDATE
  SET nimi = excluded.nimi, kuvaus = excluded.kuvaus;
