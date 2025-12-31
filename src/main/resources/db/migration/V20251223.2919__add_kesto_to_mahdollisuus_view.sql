DROP VIEW IF EXISTS mahdollisuus_view;

CREATE OR REPLACE VIEW mahdollisuus_view AS
SELECT
  m.id AS id,
  m.tyyppi AS tyyppi,
  m.ammattiryhma AS ammattiryhma,
  m.aineisto AS aineisto,
  m.otsikko AS otsikko,
  m.koulutus_tyyppi AS koulutus_tyyppi,
  ARRAY_AGG(m.maakunta) FILTER (WHERE m.maakunta IS NOT NULL) AS maakunnat,
  m.kesto AS kesto,
  m.kesto_minimi,
  m.kesto_maksimi,
  m.kieli AS kieli
FROM (
       SELECT
         t.id AS id,
         'TYOMAHDOLLISUUS' AS tyyppi,
         tk.otsikko AS otsikko,
         t.ammattiryhma AS ammattiryhma,
         t.aineisto AS aineisto,
         NULL AS koulutus_tyyppi,
         a.arvo AS maakunta,
         NULL AS kesto,
         NULL AS kesto_minimi,
         NULL AS kesto_maksimi,
         tk.kaannos_key AS kieli
       FROM tyomahdollisuus t
              JOIN tyomahdollisuus_kaannos tk ON tk.tyomahdollisuus_id = t.id
              LEFT JOIN tyomahdollisuus_jakauma j ON j.tyomahdollisuus_id = t.id AND j.tyyppi = 'MAAKUNTA'
              LEFT JOIN tyomahdollisuus_jakauma_arvot a ON a.tyomahdollisuus_jakauma_id = j.id
       WHERE t.aktiivinen = TRUE

       UNION ALL

       SELECT
         k.id AS id,
         'KOULUTUSMAHDOLLISUUS' AS tyyppi,
         kk.otsikko AS otsikko,
         NULL AS ammattiryhma,
         NULL AS aineisto,
         k.tyyppi AS koulutus_tyyppi,
         NULL AS maakunta,
         k.kesto_mediaani AS kesto,
         k.kesto_minimi AS kesto_minimi,
         k.kesto_maksimi AS kesto_maksimi,
         kk.kaannos_key AS kieli
       FROM koulutusmahdollisuus k
              JOIN koulutusmahdollisuus_kaannos kk ON kk.koulutusmahdollisuus_id = k.id
       WHERE k.aktiivinen = TRUE
     ) m
GROUP BY
  m.id, m.tyyppi, m.ammattiryhma, m.aineisto, m.koulutus_tyyppi,
  m.kesto, m.kesto_minimi, m.kesto_maksimi, m.kieli, m.otsikko;
