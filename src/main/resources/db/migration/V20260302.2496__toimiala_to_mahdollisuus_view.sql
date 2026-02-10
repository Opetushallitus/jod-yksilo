DROP VIEW mahdollisuus_view;
CREATE OR REPLACE VIEW mahdollisuus_view AS
WITH tm_location AS (SELECT j.tyomahdollisuus_id AS id, ARRAY_AGG(ja.arvo) AS maakunnat
                     FROM tyomahdollisuus_jakauma j
                            JOIN tyomahdollisuus_jakauma_arvot ja
                                 ON ja.tyomahdollisuus_jakauma_id = j.id
                     WHERE j.tyyppi = 'MAAKUNTA'
                     GROUP BY j.tyomahdollisuus_id),
     tm_industry AS (SELECT j.tyomahdollisuus_id AS id, ARRAY_AGG(ja.arvo) AS toimialat
                     FROM tyomahdollisuus_jakauma j
                            JOIN tyomahdollisuus_jakauma_arvot ja
                                 ON ja.tyomahdollisuus_jakauma_id = j.id
                     WHERE j.tyyppi = 'TOIMIALA'
                     GROUP BY j.tyomahdollisuus_id),
     km_location AS (SELECT j.koulutusmahdollisuus_id AS id, ARRAY_AGG(ja.arvo) AS maakunnat
                     FROM koulutusmahdollisuus_jakauma j
                            JOIN koulutusmahdollisuus_jakauma_arvot ja
                                 ON ja.koulutusmahdollisuus_jakauma_id = j.id
                     WHERE j.tyyppi = 'MAAKUNTA'
                     GROUP BY j.koulutusmahdollisuus_id)
SELECT t.id AS id,
       'TYOMAHDOLLISUUS' AS tyyppi,
       t.ammattiryhma AS ammattiryhma,
       t.aineisto AS aineisto,
       tk.otsikko AS otsikko,
       NULL AS koulutus_tyyppi,
       l.maakunnat AS maakunnat,
       i.toimialat as toimialat,
       NULL AS kesto,
       NULL AS kesto_minimi,
       NULL AS kesto_maksimi,
       tk.kaannos_key AS kieli
FROM tyomahdollisuus t
       JOIN tyomahdollisuus_kaannos tk ON tk.tyomahdollisuus_id = t.id
       LEFT JOIN tm_location l ON l.id = t.id
       LEFT JOIN tm_industry i ON i.id = t.id
WHERE t.aktiivinen = TRUE

UNION ALL

SELECT k.id AS id,
       'KOULUTUSMAHDOLLISUUS' AS tyyppi,
       NULL AS ammattiryhma,
       NULL AS aineisto,
       kk.otsikko AS otsikko,
       k.tyyppi AS koulutus_tyyppi,
       l.maakunnat AS maakunnat,
       NULL AS toimialat,
       k.kesto_mediaani AS kesto,
       k.kesto_minimi AS kesto_minimi,
       k.kesto_maksimi AS kesto_maksimi,
       kk.kaannos_key AS kieli
FROM koulutusmahdollisuus k
       JOIN koulutusmahdollisuus_kaannos kk ON kk.koulutusmahdollisuus_id = k.id
       LEFT JOIN km_location l ON l.id = k.id
WHERE k.aktiivinen = TRUE
;
