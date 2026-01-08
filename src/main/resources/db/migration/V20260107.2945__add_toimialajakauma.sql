ALTER TABLE tyomahdollisuus_jakauma
  DROP CONSTRAINT IF EXISTS tyomahdollisuus_jakauma_tyyppi_check;
ALTER TABLE tyomahdollisuus_jakauma
  ADD CONSTRAINT tyomahdollisuus_jakauma_tyyppi_check CHECK
    (tyyppi IN ('AMMATTI',
                'OSAAMINEN',
                'AJOKORTTI',
                'KIELITAITO',
                'KORTIT_JA_LUVAT',
                'KOULUTUSASTE',
                'RIKOSREKISTERIOTE',
                'MATKUSTUSVAATIMUS',
                'PALKAN_PERUSTE',
                'PALVELUSSUHDE',
                'TYOAIKA',
                'TYON_JATKUVUUS',
                'KUNTA',
                'MAAKUNTA',
                'MAA',
                'SIJAINTI_JOUSTAVA',
                'TYOKIELI',
                'TOIMIALA'));

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
                              ('RIKOSREKISTERIOTE', '$.osaamisvaatimukset.rikosrekisterioteJakauma'),
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
                                      x.prosenttiOsuus AS osuus
                               FROM paths p
                                      JOIN jakaumat j ON (p.n = j.tyyppi)
                                      JOIN tyomahdollisuus_data.import d
                                           ON (j.tyomahdollisuus_id = d.id),
                                    JSONB_TO_RECORDSET(JSONB_PATH_QUERY_FIRST(d.data, p.p) -> 'arvot') AS x(arvo TEXT, prosenttiOsuus FLOAT(53)))
  INSERT
  INTO tyomahdollisuus_jakauma_arvot(tyomahdollisuus_jakauma_id, arvo, osuus)
  SELECT tyomahdollisuus_jakauma_id, arvo, osuus
  FROM distribution_values;
END;
