CREATE OR REPLACE VIEW mahdollisuus_view AS
SELECT
  m.id AS id,
  m.tyyppi AS tyyppi,
  m.ammattiryhma AS ammattiryhma,
  m.aineisto AS aineisto,
  m.otsikko  AS otsikko,
  m.koulutusTyyppi AS koulutus_tyyppi,
  STRING_AGG(DISTINCT COALESCE(m.maakunta, ''), ', ') AS maakunnat,
  m.kesto AS kesto,
  m.kieli AS kieli
FROM (
       SELECT
         t.id AS id,
         tk.otsikko AS otsikko,
         'TYOMAHDOLLISUUS' AS tyyppi,
         CAST(t.ammattiryhma AS text) AS ammattiryhma,
         CAST(t.aineisto AS text) AS aineisto,
         CAST(NULL AS text) AS koulutusTyyppi,
         a.arvo AS maakunta,
         CAST(NULL AS double precision) AS kesto,
         tk.kaannos_key as kieli
       FROM tyomahdollisuus t
              JOIN tyomahdollisuus_kaannos tk ON tk.tyomahdollisuus_id = t.id
              LEFT JOIN tyomahdollisuus_jakauma j ON j.tyomahdollisuus_id = t.id AND j.tyyppi = 'MAAKUNTA'
              LEFT JOIN tyomahdollisuus_jakauma_arvot a ON a.tyomahdollisuus_jakauma_id = j.id
       WHERE  t.aktiivinen = TRUE
       UNION ALL
       SELECT
         k.id AS id,
         kk.otsikko AS otsikko,
         'KOULUTUSMAHDOLLISUUS' AS tyyppi,
         NULL AS ammattiryhma,
         NULL AS aineisto,
         CAST(k.tyyppi AS text) AS koulutusTyyppi,
         NULL AS maakunnat,
         CAST(k.kesto_mediaani AS double precision) AS kesto,
         kk.kaannos_key as kieli
       FROM koulutusmahdollisuus k
              JOIN koulutusmahdollisuus_kaannos kk ON kk.koulutusmahdollisuus_id = k.id
       WHERE k.aktiivinen = TRUE
     ) m GROUP BY m.id, m.kieli, m.tyyppi, m.ammattiryhma, m.aineisto, m.otsikko, m.koulutusTyyppi, m.kesto;
